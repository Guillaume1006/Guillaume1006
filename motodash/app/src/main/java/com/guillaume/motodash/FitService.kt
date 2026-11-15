package com.guillaume.motodash

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import kotlin.math.abs

/**
 * Ajuste la fenêtre de l'appli GPS (fenêtre de forme libre) exactement dans son cadre.
 * Android ne laisse pas une appli redimensionner la fenêtre d'une autre : on fait donc
 * comme toi avec le doigt, par des gestes simulés :
 *  1. glisser la barre de la fenêtre pour la placer en haut à gauche du cadre ;
 *  2. glisser son coin bas-droit pour lui donner la taille du cadre.
 */
class FitService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var busy = false
    private var lastWindowId = -1
    private var lastSize = Rect()
    private var sizeStuck = false
    private var lastStepResize = false
    private var lastRestore = 0L   // le GPS refuse la taille demandée (taille minimale) : on se contente de le placer

    override fun onServiceConnected() { instance = this }
    override fun onDestroy() { instance = null; super.onDestroy() }
    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (busy || OverlayService.instance == null) return
        val nav = Prefs(this).navApp ?: return
        val w = gpsWindow(nav) ?: return
        // GPS passé en plein écran (bouton ☐) : on le remet dans sa fenêtre, au plus une fois toutes les 15 s
        val b = Rect().also { w.getBoundsInScreen(it) }
        val g = Geo.compute(this)
        if (b.width() >= g.sw - 8 && b.height() >= g.sh - 8 && Frame.freeformAvailable(this)) {
            val now = SystemClock.uptimeMillis()
            if (now - lastRestore > 15_000) {
                lastRestore = now
                handler.postDelayed({ com.guillaume.motodash.ui.Drive.openGps(this, nav) }, 500)
            }
            return
        }
        // Nouvelle fenêtre GPS (ou ajustement demandé) : on la cale dans le cadre
        if (w.id != lastWindowId || SystemClock.uptimeMillis() < requestedUntil) {
            lastWindowId = w.id
            handler.removeCallbacks(fitRunnable)
            handler.postDelayed(fitRunnable, 700)
        }
    }

    private val fitRunnable = Runnable { fit(attempt = 0) }

    private fun gpsWindow(nav: String): AccessibilityWindowInfo? = runCatching {
        windows.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.root?.packageName == nav }
    }.getOrNull()

    private fun fit(attempt: Int) {
        val nav = Prefs(this).navApp ?: return
        val w = gpsWindow(nav) ?: return
        val b = Rect().also { w.getBoundsInScreen(it) }
        val g = Geo.compute(this)
        val t = g.map
        val d = resources.displayMetrics.density
        val tol = (8 * d).toInt()

        // Plein écran (pas une fenêtre libre) : rien à ajuster ici
        if (b.width() >= g.sw - tol && b.height() >= g.sh - tol) { done(); return }
        val placed = abs(b.left - t.left) <= tol && abs(b.top - t.top) <= tol
        if (attempt == 0) sizeStuck = false
        else if (lastStepResize && abs(b.width() - lastSize.width()) <= 2 && abs(b.height() - lastSize.height()) <= 2) sizeStuck = true
        lastSize = Rect(b)
        val sized = sizeStuck || (abs(b.width() - t.width()) <= tol && abs(b.height() - t.height()) <= tol)
        if ((placed && sized) || attempt >= 6) { done(); return }

        busy = true
        OverlayService.instance?.setFitting(true)   // le panneau ne doit pas gêner les gestes
        val grabY = b.top + 20 * d                   // milieu de la barre de la fenêtre
        val inset = 6 * d                            // juste à l'intérieur du coin
        val margin = 4 * d
        val cornerVisible = b.right - inset <= g.sw - margin && b.bottom - inset <= g.sh - margin
        val path = Path()
        lastStepResize = !sized && cornerVisible
        when {
            !sized && !cornerVisible -> {
                // 1. Coin bas-droit hors de l'écran : on remonte / décale la fenêtre d'abord
                stroke(path, b.exactCenterX(), grabY, minOf(0f, g.sw - margin - b.right), minOf(0f, g.sh - margin - b.bottom))
            }
            !sized -> {
                // 2. Redimensionner : coin bas-droit → taille du cadre (coin haut-gauche inchangé)
                stroke(path, b.right - inset, b.bottom - inset, (t.width() - b.width()).toFloat(), (t.height() - b.height()).toFloat())
            }
            else -> {
                // 3. Placer : barre de la fenêtre → haut-gauche du cadre
                stroke(path, b.exactCenterX(), grabY, (t.left - b.left).toFloat(), (t.top - b.top).toFloat())
            }
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 650))
            .build()
        val next = Runnable { fit(attempt + 1) }
        val ok = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) { handler.postDelayed(next, 600) }
            override fun onCancelled(gestureDescription: GestureDescription?) { handler.postDelayed(next, 600) }
        }, handler)
        if (!ok) done()
    }

    /**
     * Trajet du doigt de (sx,sy) avec un déplacement voulu (dx,dy).
     * Android « mange » les premiers pixels d'un glissé (seuil de déclenchement) :
     * on prolonge le geste d'autant pour arriver pile au bon endroit.
     */
    private fun stroke(path: Path, sx: Float, sy: Float, dx: Float, dy: Float) {
        val len = kotlin.math.hypot(dx, dy)
        val comp = if (len > 0f) (1f + 10 * resources.displayMetrics.density / len) else 1f
        path.moveTo(sx, sy)
        path.lineTo(sx + dx * comp, sy + dy * comp)
    }

    private fun done() {
        busy = false
        requestedUntil = 0
        // Le GPS a refusé la taille du cadre : c'est le cadre qui s'adapte à sa fenêtre
        val nav = Prefs(this).navApp
        val w = nav?.let { gpsWindow(it) }
        if (w != null && sizeStuck) {
            val b = Rect().also { w.getBoundsInScreen(it) }
            OverlayService.instance?.setMapOverride(b)
        }
        OverlayService.instance?.setFitting(false)
    }

    companion object {
        var instance: FitService? = null
            private set
        private var requestedUntil = 0L

        /** Demande d'ajustement (après l'ouverture du GPS par MotoDash). */
        fun request() {
            requestedUntil = SystemClock.uptimeMillis() + 10_000
            instance?.let { s -> s.handler.removeCallbacks(s.fitRunnable); s.handler.postDelayed(s.fitRunnable, 1_200) }
        }

        fun enabled() = instance != null
    }
}
