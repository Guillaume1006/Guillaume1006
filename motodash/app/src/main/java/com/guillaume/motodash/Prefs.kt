package com.guillaume.motodash

import android.content.Context
import android.content.SharedPreferences

data class Contact(val name: String, val number: String)

class Prefs(ctx: Context) {
    private val sp: SharedPreferences = ctx.getSharedPreferences("motodash", Context.MODE_PRIVATE)

    /** Appli GPS choisie (Waze, Google Maps, Calimoto…) */
    var navApp: String?
        get() = sp.getString("navApp", null)
        set(v) = sp.edit().putString("navApp", v).apply()

    /** Appli musique choisie (Spotify, Deezer, YouTube Music…) */
    var musicApp: String?
        get() = sp.getString("musicApp", null)
        set(v) = sp.edit().putString("musicApp", v).apply()

    /** Ouvrir le GPS en écran partagé au lancement de MotoDash */
    var autoLaunch: Boolean
        get() = sp.getBoolean("autoLaunch", true)
        set(v) = sp.edit().putBoolean("autoLaunch", v).apply()

    /** true : MotoDash s'affiche par-dessus le GPS (comme CarPlay). false : écran partagé Android. */
    var overlayMode: Boolean
        get() = sp.getBoolean("overlayMode", true)
        set(v) = sp.edit().putBoolean("overlayMode", v).apply()

    var askedUsage: Boolean
        get() = sp.getBoolean("askedUsage", false)
        set(v) = sp.edit().putBoolean("askedUsage", v).apply()

    var favApps: List<String>
        get() = sp.getString("favApps", "")!!.split(',').filter { it.isNotBlank() }
        set(v) = sp.edit().putString("favApps", v.joinToString(",")).apply()

    var contacts: List<Contact>
        get() = sp.getString("contacts", "")!!.lines().mapNotNull { line ->
            val p = line.split('\t')
            if (p.size == 2) Contact(p[0], p[1]) else null
        }
        set(v) = sp.edit().putString("contacts", v.joinToString("\n") { "${it.name.replace('\t', ' ')}\t${it.number}" }).apply()

    var tab: Int
        get() = sp.getInt("tab", 0)
        set(v) = sp.edit().putInt("tab", v).apply()
}
