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
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
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
import fr.cockpit.auto.data.Prefs
import fr.cockpit.auto.data.RailSide
import fr.cockpit.auto.ui.CockpitTheme
import fr.cockpit.auto.ui.RAIL_WIDTH
import fr.cockpit.auto.ui.Rail
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Barre flottante : quand une autre app est à l'écran (Waze, musique, Claude…),
 * la barre de Cockpit reste affichée par-dessus, sur le même côté.
 * Elle se cache dès que Cockpit revient au premier plan.
 */
class RailOverlayService : Service() {
    private val scope = MainScope()
    private val windowManager by lazy { getSystemService(WindowManager::class.java) }
    private var view: ComposeView? = null
    private var owner: OverlayOwner? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        scope.launch {
            combine(CockpitState.activityVisible, Prefs.railSide) { visible, side -> visible to side }
                .collect { (visible, side) -> render(show = !visible, side = side) }
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
        owner?.destroy()
        view?.let { runCatching { windowManager.removeView(it) } }
        view = null
        super.onDestroy()
    }

    private fun render(show: Boolean, side: RailSide) {
        if (!show || !Settings.canDrawOverlays(this)) {
            view?.visibility = View.GONE
            return
        }
        val v = view ?: createView().also { view = it }
        val params = v.layoutParams as WindowManager.LayoutParams
        val gravity = Gravity.TOP or if (side == RailSide.Right) Gravity.END else Gravity.START
        if (params.gravity != gravity) {
            params.gravity = gravity
            windowManager.updateViewLayout(v, params)
        }
        v.visibility = View.VISIBLE
    }

    private fun createView(): ComposeView {
        val o = OverlayOwner().also { owner = it; it.start() }
        val v = ComposeView(this).apply {
            setViewTreeLifecycleOwner(o)
            setViewTreeSavedStateRegistryOwner(o)
            setContent { CockpitTheme { FloatingRail() } }
        }
        val params = WindowManager.LayoutParams(
            (RAIL_WIDTH.value * resources.displayMetrics.density).roundToInt(),
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                // Écran allumé tant que la barre est visible (mode voiture)
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // Garde le téléphone à l'horizontale dans Waze et les autres apps,
            // même si la rotation automatique est coupée.
            screenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        windowManager.addView(v, params)
        return v
    }

    @Composable
    private fun FloatingRail() {
        val ctx: Context = this
        val claudeIcon = remember { Launchers.claudeIcon(ctx) }
        Rail(
            claudeIcon = claudeIcon,
            bottomIcon = Icons.Rounded.Home,
            bottomLabel = "Retour à Cockpit (appui long : quitter le mode voiture)",
            onNavigation = { Launchers.openNavigation(ctx) },
            onMusic = { if (!Launchers.openMusic(ctx)) Launchers.openCockpit(ctx, "settings") },
            onMessages = { Launchers.openMessages(ctx) },
            onClaude = { Launchers.openClaude(ctx) },
            onBottom = { Launchers.openCockpit(ctx) },
            onBottomLongClick = {
                Launchers.toast(ctx, "Mode voiture arrêté")
                stopSelf()
            },
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
            .setContentText("La barre Cockpit reste visible au-dessus des apps.")
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(0, "Quitter", stop)
            .build()
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    companion object {
        private const val CHANNEL = "car_mode"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "fr.cockpit.auto.STOP"

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
