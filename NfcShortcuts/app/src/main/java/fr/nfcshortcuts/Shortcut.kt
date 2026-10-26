package fr.nfcshortcuts

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class StepType(val label: String) {
    ALEXA("Alexa : routine / lumières"),
    WEB("Appel web"),
    OPEN("Ouvrir un lien ou une appli"),
    WAIT("Attendre (secondes)"),
    MESSAGE("Afficher un message"),
}

/** Une étape. [value] = URL, lien, durée ou texte selon le type ; [label] = nom lisible (Alexa). */
data class Step(val type: StepType, val value: String, val label: String = "") {
    fun describe(): String = when (type) {
        StepType.ALEXA -> "Alexa : ${label.ifBlank { "routine" }}"
        StepType.WEB -> "Appel web : $value"
        StepType.OPEN -> "Ouvrir : $value"
        StepType.WAIT -> "Attendre $value s"
        StepType.MESSAGE -> "Message : $value"
    }
}

data class Shortcut(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val steps: List<Step>,
)

class ShortcutStore(context: Context) {
    private val prefs = context.getSharedPreferences("shortcuts", Context.MODE_PRIVATE)

    fun all(): List<Shortcut> {
        val arr = JSONArray(prefs.getString("items", "[]"))
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Shortcut(o.getString("id"), o.getString("name"), readSteps(o))
        }
    }

    // Lit le nouveau format ("steps"), ou migre l'ancien ("actions" = lignes de texte).
    private fun readSteps(o: JSONObject): List<Step> {
        o.optJSONArray("steps")?.let { arr ->
            return (0 until arr.length()).map { i ->
                val s = arr.getJSONObject(i)
                Step(StepType.valueOf(s.getString("type")), s.getString("value"), s.optString("label"))
            }
        }
        val old = o.optJSONArray("actions") ?: return emptyList()
        return (0 until old.length()).map { i ->
            val line = old.getString(i)
            Step(if (line.startsWith("http")) StepType.WEB else StepType.OPEN, line)
        }
    }

    fun find(id: String): Shortcut? = all().firstOrNull { it.id == id }

    fun save(shortcut: Shortcut) = write(all().filter { it.id != shortcut.id } + shortcut)

    fun delete(id: String) = write(all().filter { it.id != id })

    private fun write(list: List<Shortcut>) {
        val arr = JSONArray()
        list.forEach { sc ->
            val steps = JSONArray()
            sc.steps.forEach { st ->
                steps.put(JSONObject().put("type", st.type.name).put("value", st.value).put("label", st.label))
            }
            arr.put(JSONObject().put("id", sc.id).put("name", sc.name).put("steps", steps))
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }
}
