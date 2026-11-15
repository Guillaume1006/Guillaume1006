package com.guillaume.motodash

import android.app.ActivityOptions
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.WindowManager
import org.lsposed.hiddenapibypass.HiddenApiBypass

/** Disposition commune : où va le GPS (le cadre) et où va le panneau MotoDash. */
data class Geo(
    val sw: Int, val sh: Int, val landscape: Boolean,
    val gap: Int, val radius: Int,
    val map: Rect, val panel: Rect,
    /** zone de l'écran non couverte par les barres système */
    val safe: Rect,
) {
    companion object {
        fun compute(ctx: Context): Geo {
            val wm = ctx.getSystemService(WindowManager::class.java)
            val sw: Int; val sh: Int
            // Zone utilisable : l'écran moins la barre d'état, la barre de navigation et l'encoche
            val safe: Rect
            if (Build.VERSION.SDK_INT >= 30) {
                val m = wm.maximumWindowMetrics
                sw = m.bounds.width(); sh = m.bounds.height()
                val i = m.windowInsets.getInsetsIgnoringVisibility(
                    android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout()
                )
                safe = Rect(i.left, i.top, sw - i.right, sh - i.bottom)
            } else {
                val pt = Point()
                @Suppress("DEPRECATION") wm.defaultDisplay.getRealSize(pt); sw = pt.x; sh = pt.y
                val id = ctx.resources.getIdentifier("status_bar_height", "dimen", "android")
                safe = Rect(0, if (id > 0) ctx.resources.getDimensionPixelSize(id) else 0, sw, sh)
            }
            val d = ctx.resources.displayMetrics.density
            val gap = (10 * d).toInt()
            val radius = (24 * d).toInt()
            val landscape = sw > sh
            return if (landscape) {
                val panelX = safe.right - (safe.width() * 0.34f).toInt()
                Geo(sw, sh, true, gap, radius,
                    map = Rect(safe.left + gap, safe.top + gap, panelX, safe.bottom - gap),
                    panel = Rect(panelX, 0, sw, sh), safe = safe)
            } else {
                val panelY = safe.bottom - (safe.height() * 0.42f).toInt()
                Geo(sw, sh, false, gap, radius,
                    map = Rect(safe.left + gap, safe.top + gap, safe.right - gap, panelY),
                    panel = Rect(0, panelY, sw, sh), safe = safe)
            }
        }
    }
}

object Frame {
    /** Fenêtres « de forme libre » disponibles (option pour les développeurs Android). */
    fun freeformAvailable(ctx: Context): Boolean =
        ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT) ||
            Settings.Global.getInt(ctx.contentResolver, "enable_freeform_support", 0) != 0

    /**
     * Options pour ouvrir le GPS exactement dans son cadre (fenêtre de forme libre).
     * Sans fenêtres de forme libre, Android ignore ces options et ouvre le GPS en plein écran.
     */
    fun gpsOptions(ctx: Context): ActivityOptions? {
        if (!freeformAvailable(ctx)) return null
        val opts = ActivityOptions.makeBasic().setLaunchBounds(Geo.compute(ctx).map)
        runCatching {
            // Mode « forme libre » (5) : fonction Android cachée, ignorée si elle n'existe pas
            if (Build.VERSION.SDK_INT >= 28) HiddenApiBypass.invoke(ActivityOptions::class.java, opts, "setLaunchWindowingMode", 5)
            else ActivityOptions::class.java.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType).invoke(opts, 5)
        }
        return opts
    }

    // ───────────── Appli au premier plan (pour masquer le panneau hors du GPS) ─────────────

    fun usageAccess(ctx: Context): Boolean {
        val ops = ctx.getSystemService(AppOpsManager::class.java)
        @Suppress("DEPRECATION")
        val mode = if (Build.VERSION.SDK_INT >= 29)
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        else ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * État récent de l'appli `pkg` : true si elle vient de passer au premier plan,
     * false si elle vient de passer en arrière-plan, null si rien ne s'est passé récemment.
     */
    fun recentState(ctx: Context, pkg: String, windowMs: Long = 5_000): Boolean? {
        val usm = ctx.getSystemService(UsageStatsManager::class.java) ?: return null
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - windowMs, now)
        val e = UsageEvents.Event()
        var state: Boolean? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.packageName != pkg) continue
            @Suppress("DEPRECATION")
            when (e.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> state = true
                UsageEvents.Event.MOVE_TO_BACKGROUND -> state = false
            }
        }
        return state
    }
}
