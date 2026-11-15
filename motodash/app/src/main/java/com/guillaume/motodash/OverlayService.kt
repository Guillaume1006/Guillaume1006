package com.guillaume.motodash

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.guillaume.motodash.ui.DashState
import com.guillaume.motodash.ui.Drive
import com.guillaume.motodash.ui.MotoTheme
import com.guillaume.motodash.ui.Panel
import com.guillaume.motodash.ui.WallpaperView
import com.guillaume.motodash.ui.Widget

/**
 * Mode « comme CarPlay » : le GPS est ouvert en plein écran et MotoDash dessine par-dessus
 *  - le panneau (1/3 droit de l'écran en paysage, bas de l'écran en portrait),
 *  - un fin cadre aux coins arrondis autour du GPS.
 * Les touches sur la carte vont au GPS ; celles sur le panneau vont à MotoDash.
 */
class OverlayService : LifecycleService(), SavedStateRegistryOwner {
    private val ssrc = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = ssrc.savedStateRegistry

    private lateinit var wm: WindowManager
    private lateinit var prefs: Prefs
    private lateinit var state: DashState
    private val windows = mutableListOf<View>()
    private var hidden = false

    override fun onCreate() {
        ssrc.performAttach()
        ssrc.performRestore(null)
        super.onCreate()
        goForeground()   // tout de suite : Android exige la notification dans les secondes qui suivent
        wm = getSystemService(WindowManager::class.java)
        prefs = Prefs(this)
        state = DashState(prefs)
        instance = this
        addWindows()
        bgHandler.post(watch)
    }

    // ───────────── Le panneau n'est visible que lorsque le GPS est au premier plan ─────────────

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    // La lecture de l'historique d'utilisation se fait hors du fil principal
    private val bg = android.os.HandlerThread("motodash-watch").apply { start() }
    private val bgHandler = android.os.Handler(bg.looper)
    private var gpsInFront = true
    private val watch = object : Runnable {
        override fun run() {
            val nav = state.navApp
            if (nav != null && Frame.usageAccess(this@OverlayService)) {
                val front = Frame.recentState(this@OverlayService, nav)
                if (front != null) handler.post {
                    if (front != gpsInFront) { gpsInFront = front; applyVisibility() }
                }
            }
            bgHandler.postDelayed(this, 700)
        }
    }

    private var fitting = false
    private var mapOverride: Rect? = null

    /** Cadre recalé sur la vraie fenêtre du GPS (quand le GPS refuse la taille demandée). */
    fun setMapOverride(r: Rect?) {
        if (r == mapOverride) return
        mapOverride = r
        removeWindows(); addWindows()
    }

    /** Caché pendant que MotoDash ajuste la fenêtre du GPS (gestes simulés). */
    fun setFitting(f: Boolean) { fitting = f; applyVisibility() }

    private fun applyVisibility() {
        val show = !hidden && gpsInFront && !fitting
        windows.forEach { it.visibility = if (show) View.VISIBLE else View.GONE }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mapOverride = null
        removeWindows(); addWindows()   // rotation : on recalcule la disposition
    }

    override fun onDestroy() {
        bgHandler.removeCallbacks(watch)
        bg.quitSafely()
        removeWindows()
        instance = null
        super.onDestroy()
    }

    /** Caché tant que l'écran MotoDash lui-même est affiché. */
    fun setHidden(h: Boolean) {
        hidden = h
        if (!h) state.reload()
        applyVisibility()
    }

    // ───────────── Fenêtres ─────────────

    private fun addWindows() {
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        var g = Geo.compute(this)
        mapOverride?.let { o ->
            // Cadre = fenêtre du GPS (limitée à l'écran), panneau = la place restante
            val m = Rect(maxOf(o.left, g.safe.left + g.gap / 2), maxOf(o.top, g.safe.top), minOf(o.right, g.sw), minOf(o.bottom, g.safe.bottom - g.gap / 2))
            if (g.landscape && g.sw - m.right >= g.sw / 4) g = g.copy(map = m, panel = Rect(m.right, 0, g.sw, g.sh))
            else if (!g.landscape && g.sh - m.bottom >= g.sh / 4) g = g.copy(map = m, panel = Rect(0, m.bottom, g.sw, g.sh))
        }
        val sw = g.sw; val sh = g.sh; val landscape = g.landscape
        val gap = g.gap; val radius = g.radius
        val d = resources.displayMetrics.density
        val panelX = g.panel.left; val panelY = g.panel.top
        val panelW = g.panel.width(); val panelH = g.panel.height()
        val mapL = g.map.left; val mapT = g.map.top; val mapR = g.map.right; val mapB = g.map.bottom

        // Cadre autour du GPS : bandes (qui bloquent les touches vers ce qu'il y a derrière) + coins arrondis
        val fw = sw.toFloat(); val fh = sh.toFloat()
        fun piece(x: Int, y: Int, w: Int, h: Int, corner: WallpaperView.Corner? = null) =
            addView(WallpaperView(this, x.toFloat(), y.toFloat(), fw, fh, corner), x, y, w, h, touchable = corner == null)
        // bandes : tout ce qui entoure le cadre du GPS (hors panneau)
        val frameR = if (landscape) panelX else sw
        val frameB = if (landscape) sh else panelY
        piece(0, 0, frameR, mapT)                          // haut
        piece(0, mapT, mapL, mapB - mapT)                  // gauche
        piece(0, mapB, frameR, frameB - mapB)              // bas
        if (mapR < frameR) piece(mapR, mapT, frameR - mapR, mapB - mapT)  // droite (portrait)
        piece(mapL, mapT, radius, radius, WallpaperView.Corner.TOP_LEFT)
        piece(mapR - radius, mapT, radius, radius, WallpaperView.Corner.TOP_RIGHT)
        piece(mapL, mapB - radius, radius, radius, WallpaperView.Corner.BOTTOM_LEFT)
        piece(mapR - radius, mapB - radius, radius, radius, WallpaperView.Corner.BOTTOM_RIGHT)

        // Panneau MotoDash
        val root = FrameLayout(this).apply {
            // Compose cherche ces « propriétaires » sur la racine de la fenêtre
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
        }
        root.addView(WallpaperView(this, panelX.toFloat(), panelY.toFloat(), fw, fh), ViewGroup.LayoutParams(-1, -1))
        val compose = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setContent {
                MotoTheme {
                    Panel(
                        state, prefs,
                        pick = { Drive.openMain(this@OverlayService, prefs, Widget.SETTINGS) },
                        modifier = Modifier.fillMaxSize().padding(
                            start = 10.dp,
                            end = ((sw - g.safe.right) / d).dp + 10.dp,
                            top = if (landscape) (g.safe.top / d).dp + 10.dp else 10.dp,
                            bottom = ((sh - g.safe.bottom) / d).dp + 10.dp,
                        ),
                        overlay = true,
                        onClose = { stopSelf() },
                    )
                }
            }
        }
        root.addView(compose, ViewGroup.LayoutParams(-1, -1))
        addView(root, panelX, panelY, panelW, panelH, touchable = true)
        applyVisibility()
    }

    private fun addView(v: View, x: Int, y: Int, w: Int, h: Int, touchable: Boolean) {
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        if (!touchable) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        val lp = WindowManager.LayoutParams(w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, flags, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x; this.y = y
            if (Build.VERSION.SDK_INT >= 30) layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            else if (Build.VERSION.SDK_INT >= 28) layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        wm.addView(v, lp)
        windows += v
    }

    private fun removeWindows() {
        windows.forEach { runCatching { wm.removeView(it) } }
        windows.clear()
    }

    private fun dimen(name: String): Int {
        val id = resources.getIdentifier(name, "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 0
    }

    // ───────────── Notification (obligatoire pour rester affiché) ─────────────

    private fun goForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "MotoDash affiché", NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(
            this, 0, Intent(this, OverlayService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE
        )
        val open = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_fg)
            .setContentTitle("MotoDash est affiché")
            .setContentText("Par-dessus ton GPS")
            .setContentIntent(open)
            .addAction(0, "Fermer", stop)
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(
            this, 1, n,
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
    }

    companion object {
        private const val CHANNEL = "overlay"
        private const val ACTION_STOP = "stop"
        var instance: OverlayService? = null
            private set

        fun start(ctx: Context) = ContextCompat.startForegroundService(ctx, Intent(ctx, OverlayService::class.java))
    }
}
