package fr.cockpit.auto.apps

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/** Ouverture des autres apps du téléphone. */
object Launchers {
    const val CLAUDE = "com.anthropic.claude"
    private const val GOOGLE_MAPS = "com.google.android.apps.maps"
    private const val WAZE = "com.waze"

    fun isInstalled(ctx: Context, pkg: String): Boolean =
        ctx.packageManager.getLaunchIntentForPackage(pkg) != null

    /** Ouvre l'app Claude. Si l'identifiant a changé, la retrouve par son nom ; sinon, Play Store. */
    fun openClaude(ctx: Context) {
        val intent = ctx.packageManager.getLaunchIntentForPackage(CLAUDE) ?: findByLabel(ctx, "Claude")
        if (intent != null && start(ctx, intent)) return
        Toast.makeText(ctx, "App Claude introuvable, ouverture du Play Store", Toast.LENGTH_SHORT).show()
        if (!start(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$CLAUDE")))) {
            start(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$CLAUDE")))
        }
    }

    /** Icône officielle de l'app Claude installée, ou null. */
    fun claudeIcon(ctx: Context): ImageBitmap? {
        val pm = ctx.packageManager
        val pkg = when {
            isInstalled(ctx, CLAUDE) -> CLAUDE
            else -> findByLabel(ctx, "Claude")?.component?.packageName ?: return null
        }
        return runCatching { pm.getApplicationIcon(pkg).toBitmap(144, 144).asImageBitmap() }.getOrNull()
    }

    /** Google Maps, sinon Waze, sinon la première app de cartes. */
    fun openNavigation(ctx: Context) {
        for (pkg in listOf(GOOGLE_MAPS, WAZE)) {
            val i = ctx.packageManager.getLaunchIntentForPackage(pkg)
            if (i != null && start(ctx, i)) return
        }
        startSelector(ctx, Intent.CATEGORY_APP_MAPS, "Aucune app de navigation trouvée")
    }

    /** Ouvre l'app qui joue en ce moment, sinon l'app de musique par défaut. */
    fun openMusic(ctx: Context, playingPackage: String?) {
        if (playingPackage != null) {
            val i = ctx.packageManager.getLaunchIntentForPackage(playingPackage)
            if (i != null && start(ctx, i)) return
        }
        startSelector(ctx, Intent.CATEGORY_APP_MUSIC, "Aucune app de musique trouvée")
    }

    fun openMessages(ctx: Context) = startSelector(ctx, Intent.CATEGORY_APP_MESSAGING, "Aucune app de messages trouvée")

    fun openCalendar(ctx: Context) = startSelector(ctx, Intent.CATEGORY_APP_CALENDAR, "Aucun agenda trouvé")

    fun openDialer(ctx: Context) {
        if (!start(ctx, Intent(Intent.ACTION_DIAL))) toast(ctx, "Aucune app téléphone trouvée")
    }

    fun openAppSettings(ctx: Context) {
        start(ctx, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
    }

    fun openNotificationAccess(ctx: Context) {
        start(ctx, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun startSelector(ctx: Context, category: String, error: String) {
        if (!start(ctx, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, category))) toast(ctx, error)
    }

    private fun findByLabel(ctx: Context, label: String): Intent? {
        val pm = ctx.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val app = pm.queryIntentActivities(main, 0)
            .firstOrNull { it.loadLabel(pm).toString().equals(label, ignoreCase = true) } ?: return null
        return Intent(main).setClassName(app.activityInfo.packageName, app.activityInfo.name)
    }

    private fun start(ctx: Context, intent: Intent): Boolean = try {
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }

    private fun toast(ctx: Context, text: String) = Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show()
}
