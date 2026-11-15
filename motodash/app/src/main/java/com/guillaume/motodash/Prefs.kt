package com.guillaume.motodash

import android.content.Context
import android.content.SharedPreferences

data class Contact(val name: String, val number: String)

class Prefs(ctx: Context) {
    private val sp: SharedPreferences = ctx.getSharedPreferences("motodash", Context.MODE_PRIVATE)

    var favApps: List<String>
        get() = sp.getString("favApps", "")!!.split(',').filter { it.isNotBlank() }
        set(v) = sp.edit().putString("favApps", v.joinToString(",")).apply()

    var contacts: List<Contact>
        get() = sp.getString("contacts", "")!!.lines().mapNotNull { line ->
            val p = line.split('\t')
            if (p.size == 2) Contact(p[0], p[1]) else null
        }
        set(v) = sp.edit().putString("contacts", v.joinToString("\n") { "${it.name.replace('\t', ' ')}\t${it.number}" }).apply()

    var navApp: String?
        get() = sp.getString("navApp", null)
        set(v) = sp.edit().putString("navApp", v).apply()

    var nightMap: Boolean
        get() = sp.getBoolean("nightMap", false)
        set(v) = sp.edit().putBoolean("nightMap", v).apply()

    var headingUp: Boolean
        get() = sp.getBoolean("headingUp", false)
        set(v) = sp.edit().putBoolean("headingUp", v).apply()

    var mapFirst: Boolean
        get() = sp.getBoolean("mapFirst", true)
        set(v) = sp.edit().putBoolean("mapFirst", v).apply()

    var tab: Int
        get() = sp.getInt("tab", 0)
        set(v) = sp.edit().putInt("tab", v).apply()
}
