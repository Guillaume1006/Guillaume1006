package com.guillaume.motodash.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.guillaume.motodash.Apps
import com.guillaume.motodash.Frame
import com.guillaume.motodash.MainActivity
import com.guillaume.motodash.OverlayService
import com.guillaume.motodash.Prefs

/** Démarrage du mode conduite : ouvre le GPS et affiche MotoDash à côté. */
object Drive {
    fun start(ctx: Context, state: DashState) {
        val nav = state.navApp ?: return
        if (state.overlayMode) {
            if (!Settings.canDrawOverlays(ctx)) {
                Toast.makeText(ctx, "Autorise MotoDash à s'afficher par-dessus les autres applis", Toast.LENGTH_LONG).show()
                ctx.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + ctx.packageName))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return
            }
            val prefs = Prefs(ctx)
            if (!Frame.usageAccess(ctx) && !prefs.askedUsage) {
                // Une seule fois : sans cet accès, le panneau reste affiché même hors du GPS
                prefs.askedUsage = true
                Toast.makeText(ctx, "Active MotoDash pour que le panneau se cache quand tu quittes le GPS", Toast.LENGTH_LONG).show()
                ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            }
            OverlayService.start(ctx)
            openGps(ctx, nav)
        } else {
            Apps.launch(ctx, nav, adjacent = true)    // écran partagé Android
        }
    }

    /** GPS dans son cadre (fenêtre de forme libre si disponible, sinon plein écran sous le panneau). */
    fun openGps(ctx: Context, nav: String) = Apps.launch(ctx, nav, adjacent = false, options = Frame.gpsOptions(ctx))

    /** Ouvre l'écran MotoDash (réglages, choix d'applis, ajout de contacts). */
    fun openMain(ctx: Context, prefs: Prefs, tab: Widget? = null) {
        if (tab != null) prefs.tab = tab.ordinal
        ctx.startActivity(Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
