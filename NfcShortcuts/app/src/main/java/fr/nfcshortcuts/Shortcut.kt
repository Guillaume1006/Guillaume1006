package fr.nfcshortcuts

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Un raccourci = un nom + une liste d'actions (une par ligne : http(s) = appel web, autre = ouvrir le lien/l'appli). */
data class Shortcut(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val actions: List<String>,
)

class ShortcutStore(context: Context) {
    private val prefs = context.getSharedPreferences("shortcuts", Context.MODE_PRIVATE)

    fun all(): List<Shortcut> {
        val arr = JSONArray(prefs.getString("items", "[]"))
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val acts = o.getJSONArray("actions")
            Shortcut(o.getString("id"), o.getString("name"), (0 until acts.length()).map { acts.getString(it) })
        }
    }

    fun find(id: String): Shortcut? = all().firstOrNull { it.id == id }

    fun save(shortcut: Shortcut) {
        val list = all().filter { it.id != shortcut.id } + shortcut
        write(list)
    }

    fun delete(id: String) = write(all().filter { it.id != id })

    private fun write(list: List<Shortcut>) {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(JSONObject().put("id", s.id).put("name", s.name).put("actions", JSONArray(s.actions)))
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }
}
