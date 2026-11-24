package fr.cockpit.auto.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import fr.cockpit.auto.MainActivity
import fr.cockpit.auto.R
import fr.cockpit.auto.apps.Launchers
import fr.cockpit.auto.data.CockpitState
import fr.cockpit.auto.data.ForegroundApp
import fr.cockpit.auto.data.Prefs
import fr.cockpit.auto.data.RailSide
import fr.cockpit.auto.media.NowPlaying
import fr.cockpit.auto.ui.CockpitTheme
import fr.cockpit.auto.ui.DashboardSide
import fr.cockpit.auto.ui.K
import fr.cockpit.auto.ui.MAP_WEIGHT
import fr.cockpit.auto.ui.RAIL_WIDTH
import fr.cockpit.auto.ui.Rail
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Fenêtres de Cockpit affichées par-dessus les autres apps :
 * - la barre latérale, au-dessus de Waze, Claude et des apps ouvertes depuis Cockpit ;
 * - dans Waze, la colonne musique / agenda / météo à droite : Waze n'est visible qu'au milieu,
 *   à la place de la carte, comme sur le tableau de bord.
 * Tout se cache dès qu'on revient dans Cockpit, ou qu'on part dans les menus du téléphone.
 */
class RailOverlayService : Service() {
    private val scope = MainScope()
    private val windowManager by lazy { getSystemService(WindowManager::class.java) }
    private var railView: ComposeView? = null
    private var panelView: ComposeView? = null
    private val owner = OverlayOwner()
    private val nowPlaying by lazy { NowPlaying(applicationContext) }

    /** Dernière app vue au premier plan, et jusqu'à quand on a déjà lu l'historique. */
    private var foreground: String? = null
    private var readUntil = System.currentTimeMillis() - 10 * 60_000L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        owner.start()
        startAsForeground()
        // Réaction immédiate : retour dans Cockpit, ouverture d'une app, réglages
        scope.launch {
            combine(CockpitState.activityVisible, CockpitState.launchTick, Prefs.railSide, Prefs.wazePanel) { _, _, _, _ -> }
                .collect { refresh() }
        }
        // Suivi de l'app au premier plan (sortie vers les menus du téléphone)
        scope.launch {
            while (isActive) {
                delay(400)
                refresh()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_NOT_STICKY
    }

    /** Cockpit balayé hors des apps récentes : on retire la barre. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        nowPlaying.stop()
        owner.destroy()
        listOfNotNull(railView, panelView).forEach { runCatching { windowManager.removeView(it) } }
        railView = null
        panelView = null
        super.onDestroy()
    }

    private fun refresh() {
        val inGrace = SystemClock.elapsedRealtime() - CockpitState.lastLaunchAt < LAUNCH_GRACE_MS
        val fg = currentForeground(inGrace)
        val rail = shouldShowRail(inGrace, fg)
        val panel = rail && Prefs.wazePanel.value && isNavigation(fg)
        render(rail, panel, Prefs.railSide.value)
    }

    /** App au premier plan, ou null si on ne peut pas le savoir. */
    private fun currentForeground(inGrace: Boolean): String? {
        // Juste après une ouverture, l'historique Android n'est pas encore à jour.
        if (inGrace) return CockpitState.lastLaunchedPackage
        val detector = ForegroundApp.detector
        if (!detector.hasAccess(this)) return null
        val now = System.currentTimeMillis()
        detector.lastResumed(this, readUntil - 2_000)?.let { foreground = it }
        readUntil = now
        return foreground
    }

    private fun shouldShowRail(inGrace: Boolean, fg: String?): Boolean {
        if (!Settings.canDrawOverlays(this)) return false
        // Juste après une ouverture : affichée tout de suite, avant même que l'app ne s'affiche.
        // Elle recouvre exactement la barre de Cockpit : aucun saut visible.
        if (inGrace) return true
        if (CockpitState.activityVisible.value) return false
        // Sans l'accès aux données d'utilisation, on ne sait pas où on est : on garde la barre.
        if (fg == null) return true
        return fg in CockpitState.carApps || fg == packageName
    }

    private fun isNavigation(fg: String?): Boolean =
        (fg ?: CockpitState.lastLaunchedPackage) in Launchers.NAVIGATION_APPS

    private fun render(rail: Boolean, panel: Boolean, side: RailSide) {
        val railGravity = Gravity.TOP or if (side == RailSide.Right) Gravity.END else Gravity.START
        val panelGravity = Gravity.TOP or if (side == RailSide.Right) Gravity.START else Gravity.END
        show(rail, railGravity, { railView }, { createWindow(TAG_RAIL, railWidthPx()) { FloatingRail() }.also { railView = it } })
        show(panel, panelGravity, { panelView }, {
            nowPlaying.start()
            createWindow(TAG_PANEL, panelWidthPx()) { WazePanel() }.also { panelView = it }
        })
    }

    private fun show(visible: Boolean, gravity: Int, current: () -> ComposeView?, create: () -> ComposeView) {
        if (!visible) {
            current()?.visibility = View.GONE
            return
        }
        val v = current() ?: create()
        val params = v.layoutParams as WindowManager.LayoutParams
        if (params.gravity != gravity) {
            params.gravity = gravity
            windowManager.updateViewLayout(v, params)
        }
        v.visibility = View.VISIBLE
    }

    private fun createWindow(tag: String, widthPx: Int, content: @Composable () -> Unit): ComposeView {
        val v = ComposeView(this).apply {
            this.tag = tag
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent { CockpitTheme { content() } }
        }
        val params = WindowManager.LayoutParams(
            widthPx,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                // Écran allumé tant que Cockpit est affiché (mode voiture)
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // Collée au bord de l'écran, même du côté de la caméra
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            // Garde le téléphone à l'horizontale, même rotation automatique coupée
            screenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        windowManager.addView(v, params)
        return v
    }

    private fun railWidthPx() = (RAIL_WIDTH.value * resources.displayMetrics.density).roundToInt()

    /**
     * Largeur de la colonne de droite, calculée comme sur le tableau de bord :
     * Waze occupe ainsi exactement la place de la carte.
     */
    internal fun panelWidthPx(): Int {
        val dm = resources.displayMetrics
        val d = dm.density
        val screenDp = max(dm.widthPixels, dm.heightPixels) / d
        val inner = screenDp - RAIL_WIDTH.value - 2 * PADDING_DP // contenu du tableau de bord
        val column = (inner - PADDING_DP) / (MAP_WEIGHT + 1f)      // colonne de droite
        return ((column + 2 * PADDING_DP) * d).roundToInt()         // + écart avec la carte + bord
    }

    @Composable
    private fun FloatingRail() {
        val ctx: Context = this
        val claudeIcon = remember { Launchers.claudeIcon(ctx) }
        Rail(
            claudeIcon = claudeIcon,
            selected = null,
            bottomIcon = Icons.Rounded.Home,
            bottomLabel = "Retour à Cockpit (appui long : quitter le mode voiture)",
            onNavigation = { Launchers.openNavigation(ctx) },
            onMusic = { Launchers.openCockpit(ctx, "music") },
            onMessages = { Launchers.openCockpit(ctx, "messages") },
            onClaude = { Launchers.openClaude(ctx) },
            onBottom = { Launchers.openCockpit(ctx) },
            onBottomLongClick = {
                Launchers.toast(ctx, "Mode voiture arrêté")
                stopSelf()
            },
        )
    }

    @Composable
    private fun WazePanel() {
        val ctx: Context = this
        DashboardSide(
            nowPlaying = nowPlaying,
            refresh = 0,
            onOpenMusic = { Launchers.openCockpit(ctx, "music") },
            modifier = Modifier
                .fillMaxSize()
                .background(K.bg)
                .padding(PADDING_DP.dp),
        )
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Mode voiture", NotificationManager.IMPORTANCE_LOW)
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, RailOverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val open = PendingIntent.getActivity(
            this, 2, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Mode voiture actif")
            .setContentText("Cockpit reste affiché autour de Waze et au-dessus de Claude.")
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(0, "Quitter", stop)
            .build()
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    companion object {
        const val TAG_RAIL = "rail"
        const val TAG_PANEL = "panel"
        private const val CHANNEL = "car_mode"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "fr.cockpit.auto.STOP"
        private const val LAUNCH_GRACE_MS = 2_500L
        /** Marge du tableau de bord (bords et écart entre la carte et la colonne). */
        private const val PADDING_DP = 12f

        /** À appeler quand Cockpit est à l'écran (démarrage autorisé au premier plan). */
        fun startIfAllowed(ctx: Context) {
            if (Settings.canDrawOverlays(ctx)) {
                ContextCompat.startForegroundService(ctx, Intent(ctx, RailOverlayService::class.java))
            }
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, RailOverlayService::class.java))
        }
    }
}

/** Cycle de vie minimal dont Compose a besoin hors d'une activité. */
private class OverlayOwner : SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

    fun start() {
        savedState.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}
