package fr.cockpit.auto.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RailSide { Left, Right }

/** Choix de l'utilisateur, gardés sur le téléphone. */
object Prefs {
    private var sp: SharedPreferences? = null

    private val _musicPackage = MutableStateFlow<String?>(null)
    /** App ouverte par le bouton Musique (null tant qu'elle n'est pas choisie). */
    val musicPackage: StateFlow<String?> = _musicPackage.asStateFlow()

    private val _railSide = MutableStateFlow(RailSide.Left)
    val railSide: StateFlow<RailSide> = _railSide.asStateFlow()

    private var app: Context? = null

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

    /** Écran demandé depuis la barre flottante ("dashboard", "apps", "settings"). */
    val requestedScreen = MutableStateFlow<String?>(null)
}
