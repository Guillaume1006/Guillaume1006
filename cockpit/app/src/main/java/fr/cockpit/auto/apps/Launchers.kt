package fr.cockpit.auto.apps

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import fr.cockpit.auto.MainActivity
import fr.cockpit.auto.data.Prefs

data class AppInfo(val packageName: String, val label: String, val icon: ImageBitmap?)

/** Ouverture des autres apps du téléphone. */
object Launchers {
    const val CLAUDE = "com.anthropic.claude"
    private const val WAZE = "com.waze"
    private const val GOOGLE_MAPS = "com.google.android.apps.maps"

    fun isInstalled(ctx: Context, pkg: String): Boolean =
        ctx.packageManager.getLaunchIntentForPackage(pkg) != null

    /* ---------- Claude ---------- */

    /** Ouvre l'app Claude. Si l'identifiant a changé, la retrouve par son nom ; sinon, Play Store. */
    fun openClaude(ctx: Context) {
        val intent = ctx.packageManager.getLaunchIntentForPackage(CLAUDE) ?: findByLabel(ctx, "Claude")
        if (intent != null && start(ctx, intent)) return
        toast(ctx, "App Claude introuvable, ouverture du Play Store")
        if (!start(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$CLAUDE")))) {
            start(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$CLAUDE")))
        }
    }

    /** Icône officielle de l'app Claude installée, ou null. */
    fun claudeIcon(ctx: Context): ImageBitmap? {
        val pkg = if (isInstalled(ctx, CLAUDE)) CLAUDE
        else findByLabel(ctx, "Claude")?.component?.packageName ?: return null
        return appIcon(ctx, pkg)
    }

    /* ---------- Navigation : Waze ---------- */

    /** Waze, sinon Google Maps, sinon la première app de cartes. */
    fun openNavigation(ctx: Context) {
        for (pkg in listOf(WAZE, GOOGLE_MAPS)) {
            val i = ctx.packageManager.getLaunchIntentForPackage(pkg)
            if (i != null && start(ctx, i)) return
        }
        startSelector(ctx, Intent.CATEGORY_APP_MAPS, "Waze n'est pas installé")
    }

    fun hasWaze(ctx: Context) = isInstalled(ctx, WAZE)

    /* ---------- Musique : l'app choisie dans les réglages ---------- */

    /** Ouvre l'app de musique choisie. Renvoie false si aucune n'est choisie (ou plus installée). */
    fun openMusic(ctx: Context): Boolean {
        val pkg = Prefs.musicPackage.value ?: return false
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        return start(ctx, i)
    }

    /** Apps capables de jouer de la musique : celles compatibles Android Auto, plus les lecteurs déclarés. */
    fun musicApps(ctx: Context): List<AppInfo> {
        val pm = ctx.packageManager
        val pkgs = mutableSetOf<String>()
        pm.queryIntentServices(Intent("android.media.browse.MediaBrowserService"), 0)
            .mapTo(pkgs) { it.serviceInfo.packageName }
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC), 0)
            .mapTo(pkgs) { it.activityInfo.packageName }
        return pkgs
            .filter { it != ctx.packageName && pm.getLaunchIntentForPackage(it) != null }
            .map { AppInfo(it, label(pm, it), appIcon(ctx, it)) }
            .sortedBy { it.label.lowercase() }
    }

    /* ---------- Autres apps ---------- */

    fun openMessages(ctx: Context) = startSelector(ctx, Intent.CATEGORY_APP_MESSAGING, "Aucune app de messages trouvée")

    fun openCalendar(ctx: Context) = startSelector(ctx, Intent.CATEGORY_APP_CALENDAR, "Aucun agenda trouvé")

    fun openDialer(ctx: Context) {
        if (!start(ctx, Intent(Intent.ACTION_DIAL))) toast(ctx, "Aucune app téléphone trouvée")
    }

    /** Ramène Cockpit au premier plan, sur l'écran demandé. */
    fun openCockpit(ctx: Context, screen: String = "dashboard") {
        start(ctx, Intent(ctx, MainActivity::class.java).putExtra(MainActivity.EXTRA_SCREEN, screen))
    }

    /* ---------- Réglages Android ---------- */

    fun openAppSettings(ctx: Context) {
        start(ctx, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
    }

    fun openNotificationAccess(ctx: Context) {
        start(ctx, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    fun openOverlayPermission(ctx: Context) {
        start(ctx, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
    }

    /* ---------- Outils ---------- */

    fun appIcon(ctx: Context, pkg: String): ImageBitmap? = runCatching {
        ctx.packageManager.getApplicationIcon(pkg).toBitmap(144, 144).asImageBitmap()
    }.getOrNull()

    private fun label(pm: PackageManager, pkg: String): String = runCatching {
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

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

    fun toast(ctx: Context, text: String) = Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show()
}
