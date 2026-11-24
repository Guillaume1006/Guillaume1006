package fr.cockpit.auto.apps

import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import fr.cockpit.auto.MainActivity
import fr.cockpit.auto.data.CockpitState
import fr.cockpit.auto.data.Prefs

data class AppInfo(val packageName: String, val label: String, val icon: ImageBitmap?)

/**
 * Ouverture des autres apps. Tout s'ouvre sans animation et la barre flottante
 * est affichée avant, au même endroit : on ne voit pas de changement d'app.
 */
object Launchers {
    const val CLAUDE = "com.anthropic.claude"
    const val WAZE = "com.waze"
    private const val GOOGLE_MAPS = "com.google.android.apps.maps"

    fun isInstalled(ctx: Context, pkg: String): Boolean =
        ctx.packageManager.getLaunchIntentForPackage(pkg) != null

    /* ---------- Claude ---------- */

    /** Ouvre l'app Claude. Si l'identifiant a changé, la retrouve par son nom ; sinon, Play Store. */
    fun openClaude(ctx: Context) {
        val intent = ctx.packageManager.getLaunchIntentForPackage(CLAUDE) ?: findByLabel(ctx, "Claude")
        if (intent != null && openApp(ctx, intent)) return
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
            if (i != null && openApp(ctx, i)) return
        }
        if (!start(ctx, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MAPS))) {
            toast(ctx, "Waze n'est pas installé")
        }
    }

    /* ---------- Musique ---------- */

    /** Ouvre l'app de musique choisie (pour parcourir ses playlists). */
    fun openMusicApp(ctx: Context): Boolean {
        val pkg = Prefs.musicPackage.value ?: return false
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        return openApp(ctx, i)
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
            .map { AppInfo(it, label(ctx, it), appIcon(ctx, it)) }
            .sortedBy { it.label.lowercase() }
    }

    /* ---------- Téléphone ---------- */

    fun call(ctx: Context, number: String) {
        markDialer(ctx)
        if (!start(ctx, Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(number))))) {
            start(ctx, Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number))))
        }
    }

    fun openDialer(ctx: Context) {
        markDialer(ctx)
        if (!start(ctx, Intent(Intent.ACTION_DIAL))) toast(ctx, "Aucune app téléphone trouvée")
    }

    /** L'écran d'appel garde la barre flottante. */
    private fun markDialer(ctx: Context) {
        ctx.getSystemService(TelecomManager::class.java)?.defaultDialerPackage?.let { CockpitState.markLaunched(it) }
    }

    /* ---------- Cockpit ---------- */

    /** Ramène Cockpit au premier plan, sur l'écran demandé, sans animation. */
    fun openCockpit(ctx: Context, screen: String = "dashboard") {
        start(ctx, Intent(ctx, MainActivity::class.java).putExtra(MainActivity.EXTRA_SCREEN, screen))
    }

    fun openCalendar(ctx: Context) {
        val i = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)
        if (!start(ctx, i)) toast(ctx, "Aucun agenda trouvé")
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

    fun openUsageAccess(ctx: Context) {
        if (!start(ctx, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:${ctx.packageName}")))) {
            start(ctx, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    /* ---------- Outils ---------- */

    fun appIcon(ctx: Context, pkg: String): ImageBitmap? = runCatching {
        ctx.packageManager.getApplicationIcon(pkg).toBitmap(144, 144).asImageBitmap()
    }.getOrNull()

    fun label(ctx: Context, pkg: String): String = runCatching {
        val pm = ctx.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    /** Ouvre une app externe : barre flottante affichée d'abord, puis l'app sans animation. */
    private fun openApp(ctx: Context, intent: Intent): Boolean {
        val pkg = intent.component?.packageName ?: intent.`package`
        if (pkg != null) CockpitState.markLaunched(pkg)
        return start(ctx, intent)
    }

    private fun findByLabel(ctx: Context, label: String): Intent? {
        val pm = ctx.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val app = pm.queryIntentActivities(main, 0)
            .firstOrNull { it.loadLabel(pm).toString().equals(label, ignoreCase = true) } ?: return null
        return Intent(main).setClassName(app.activityInfo.packageName, app.activityInfo.name)
    }

    private fun start(ctx: Context, intent: Intent): Boolean = try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        ctx.startActivity(intent, ActivityOptions.makeCustomAnimation(ctx, 0, 0).toBundle())
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }

    fun toast(ctx: Context, text: String) = Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show()
}
