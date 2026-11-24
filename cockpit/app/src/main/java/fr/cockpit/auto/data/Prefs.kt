package fr.cockpit.auto.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Process
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections

enum class RailSide { Left, Right }

/** Choix de l'utilisateur, gardés sur le téléphone. */
object Prefs {
    private var sp: SharedPreferences? = null
    private var app: Context? = null

    private val _musicPackage = MutableStateFlow<String?>(null)
    /** App de musique pilotée par le lecteur de Cockpit (null tant qu'elle n'est pas choisie). */
    val musicPackage: StateFlow<String?> = _musicPackage.asStateFlow()

    private val _railSide = MutableStateFlow(RailSide.Left)
    val railSide: StateFlow<RailSide> = _railSide.asStateFlow()

    fun init(ctx: Context) {
        if (sp != null && app === ctx.applicationContext) return
        app = ctx.applicationContext
        val prefs = ctx.applicationContext.getSharedPreferences("cockpit", Context.MODE_PRIVATE)
        sp = prefs
        _musicPackage.value = prefs.getString(KEY_MUSIC, null)
        _railSide.value = if (prefs.getString(KEY_SIDE, null) == "right") RailSide.Right else RailSide.Left
    }

    fun setMusicPackage(pkg: String) {
        _musicPackage.value = pkg
        sp?.edit()?.putString(KEY_MUSIC, pkg)?.apply()
    }

    fun setRailSide(side: RailSide) {
        _railSide.value = side
        sp?.edit()?.putString(KEY_SIDE, if (side == RailSide.Right) "right" else "left")?.apply()
    }

    private const val KEY_MUSIC = "music_package"
    private const val KEY_SIDE = "rail_side"
}

/** État partagé entre l'écran principal et la barre flottante. */
object CockpitState {
    /** Vrai quand Cockpit est à l'écran : la barre flottante se cache alors. */
    val activityVisible = MutableStateFlow(false)

    /** Écran demandé depuis la barre flottante ("dashboard", "music", "messages", "settings"…). */
    val requestedScreen = MutableStateFlow<String?>(null)

    /** Apps ouvertes depuis Cockpit (Waze, Claude…) : la barre flottante ne s'affiche qu'au-dessus d'elles. */
    val carApps: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    /** Moment de la dernière ouverture d'app depuis Cockpit (horloge depuis le démarrage). */
    @Volatile var lastLaunchAt = 0L

    /** Change à chaque ouverture : la barre flottante s'affiche sans attendre. */
    val launchTick = MutableStateFlow(0)

    fun markLaunched(pkg: String) {
        carApps += pkg
        lastLaunchAt = SystemClock.elapsedRealtime()
        launchTick.value++
    }
}

/** Quelle app est au premier plan ? (permission « Accès aux données d'utilisation »). */
interface ForegroundDetector {
    fun hasAccess(ctx: Context): Boolean
    /** Dernière app passée au premier plan depuis [sinceMs], ou null si aucune. */
    fun lastResumed(ctx: Context, sinceMs: Long): String?
}

object ForegroundApp {
    /** Remplaçable dans les tests. */
    var detector: ForegroundDetector = UsageStatsDetector
}

private object UsageStatsDetector : ForegroundDetector {
    override fun hasAccess(ctx: Context): Boolean {
        val ops = ctx.getSystemService(AppOpsManager::class.java) ?: return false
        return ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName) ==
            AppOpsManager.MODE_ALLOWED
    }

    override fun lastResumed(ctx: Context, sinceMs: Long): String? {
        if (!hasAccess(ctx)) return null
        val usm = ctx.getSystemService(UsageStatsManager::class.java) ?: return null
        val events = usm.queryEvents(sinceMs, System.currentTimeMillis()) ?: return null
        val e = UsageEvents.Event()
        var pkg: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.ACTIVITY_RESUMED) pkg = e.packageName
        }
        return pkg
    }
}
