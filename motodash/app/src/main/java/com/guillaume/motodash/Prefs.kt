package com.guillaume.motodash

import android.content.Context
import android.content.SharedPreferences
import com.guillaume.motodash.nav.Place

data class Contact(val name: String, val number: String)

class Prefs(ctx: Context) {
    private val sp: SharedPreferences = ctx.getSharedPreferences("motodash", Context.MODE_PRIVATE)

    /** Appli musique choisie (Spotify, Deezer, YouTube Music…) */
    var musicApp: String?
        get() = sp.getString("musicApp", null)
        set(v) = sp.edit().putString("musicApp", v).apply()

    /** Guidage vocal */
    var voice: Boolean
        get() = sp.getBoolean("voice", true)
        set(v) = sp.edit().putBoolean("voice", v).apply()

    var avoidHighways: Boolean
        get() = sp.getBoolean("avoidHighways", false)
        set(v) = sp.edit().putBoolean("avoidHighways", v).apply()

    var avoidTolls: Boolean
        get() = sp.getBoolean("avoidTolls", false)
        set(v) = sp.edit().putBoolean("avoidTolls", v).apply()

    var favorites: List<Place>
        get() = places("favorites")
        set(v) = savePlaces("favorites", v)

    var recents: List<Place>
        get() = places("recents")
        set(v) = savePlaces("recents", v)

    fun addRecent(p: Place) { recents = (listOf(p) + recents.filterNot { it.name == p.name && it.detail == p.detail }).take(6) }

    private fun places(key: String) = sp.getString(key, "")!!.split('\u001E').mapNotNull { Place.decode(it) }
    private fun savePlaces(key: String, v: List<Place>) = sp.edit().putString(key, v.joinToString("\u001E") { it.encode() }).apply()

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
