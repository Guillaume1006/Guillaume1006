package com.guillaume.motodash

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppEntry(val pkg: String, val label: String, val icon: ImageBitmap)

object Apps {
    private val cache = mutableMapOf<String, AppEntry>()

    fun all(ctx: Context): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return ctx.packageManager.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName }
            .toEntries(ctx)
    }

    /** Applis capables de guider vers une adresse (Waze, Google Maps, OsmAnd, Calimoto…). */
    fun navApps(ctx: Context): List<AppEntry> =
        ctx.packageManager.queryIntentActivities(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=Paris")), 0)
            .map { it.activityInfo.packageName }
            .toEntries(ctx)

    /** Applis qui jouent de la musique (Spotify, Deezer, YouTube Music…). */
    fun musicApps(ctx: Context): List<AppEntry> {
        val pm = ctx.packageManager
        val receivers = pm.queryBroadcastReceivers(Intent(Intent.ACTION_MEDIA_BUTTON), 0).map { it.activityInfo.packageName }
        val browsers = pm.queryIntentServices(Intent("android.media.browse.MediaBrowserService"), 0).map { it.serviceInfo.packageName }
        return (receivers + browsers).toEntries(ctx)
    }

    private fun List<String>.toEntries(ctx: Context) = distinct()
        .filter { it != ctx.packageName && ctx.packageManager.getLaunchIntentForPackage(it) != null }
        .mapNotNull { entry(ctx, it) }
        .sortedBy { it.label.lowercase() }

    fun entry(ctx: Context, pkg: String?): AppEntry? {
        if (pkg == null) return null
        return cache[pkg] ?: try {
            val pm = ctx.packageManager
            val info = pm.getApplicationInfo(pkg, 0)
            AppEntry(pkg, pm.getApplicationLabel(info).toString(), pm.getApplicationIcon(info).toBitmap(128, 128).asImageBitmap())
                .also { cache[pkg] = it }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Ouvre une appli.
     * - adjacent = true (le GPS) : l'appli s'ouvre dans l'AUTRE moitié de l'écran partagé.
     *   Sur Android 12L et plus, cela peut aussi lancer l'écran partagé tout seul.
     * - adjacent = false (musique, autres) : l'appli s'ouvre à la place de MotoDash, le GPS reste visible.
     */
    fun launch(ctx: Context, pkg: String, adjacent: Boolean = false, options: android.app.ActivityOptions? = null) {
        val intent = ctx.packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null) {
            Toast.makeText(ctx, "Appli introuvable", Toast.LENGTH_SHORT).show()
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (adjacent) intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        ctx.startActivity(intent, options?.toBundle())
    }
}
