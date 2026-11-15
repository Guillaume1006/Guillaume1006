package com.guillaume.motodash

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppEntry(val pkg: String, val label: String, val icon: ImageBitmap)

object Apps {
    private val cache = mutableMapOf<String, AppEntry>()

    fun all(ctx: Context): List<AppEntry> {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != ctx.packageName }
            .mapNotNull { entry(ctx, it) }
            .sortedBy { it.label.lowercase() }
    }

    fun entry(ctx: Context, pkg: String): AppEntry? = cache[pkg] ?: try {
        val pm = ctx.packageManager
        val info = pm.getApplicationInfo(pkg, 0)
        AppEntry(pkg, pm.getApplicationLabel(info).toString(), pm.getApplicationIcon(info).toBitmap(128, 128).asImageBitmap())
            .also { cache[pkg] = it }
    } catch (_: Exception) {
        null
    }

    /**
     * Ouvre une appli. Si MotoDash est en écran partagé, Android l'affiche dans l'autre moitié
     * (FLAG_ACTIVITY_LAUNCH_ADJACENT) et la carte reste visible.
     */
    fun launch(ctx: Context, pkg: String) {
        val intent = ctx.packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null) {
            Toast.makeText(ctx, "Appli introuvable", Toast.LENGTH_SHORT).show()
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
        val split = (ctx as? Activity)?.isInMultiWindowMode == true
        if (!split) {
            Toast.makeText(
                ctx,
                "Astuce : mets MotoDash en écran partagé pour garder la carte à côté",
                Toast.LENGTH_LONG,
            ).show()
        }
        ctx.startActivity(intent)
    }
}
