package fr.nfcshortcuts

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Éditeur d'un raccourci : un nom + une liste d'étapes réordonnables. */
class EditorActivity : Activity() {

    private lateinit var store: ShortcutStore
    private lateinit var nameField: EditText
    private lateinit var stepsBox: LinearLayout
    private val steps = mutableListOf<Step>()
    private var id: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = ShortcutStore(this)
        id = intent.getStringExtra("id")
        val existing = id?.let { store.find(it) }
        if (existing != null) steps.addAll(existing.steps)

        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        nameField = EditText(this).apply {
            hint = "Nom du raccourci (ex : Départ maison)"
            setText(existing?.name ?: "")
        }
        stepsBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val add = Button(this).apply {
            text = "+ Ajouter une étape"
            setOnClickListener { chooseType() }
        }
        val save = Button(this).apply {
            text = "Enregistrer"
            setOnClickListener { save() }
        }
        root.addView(nameField)
        root.addView(TextView(this).apply { text = "Étapes (exécutées dans l'ordre)"; setPadding(0, pad, 0, 0) })
        root.addView(stepsBox)
        root.addView(add)
        root.addView(save)
        setContentView(ScrollView(this).apply {
            addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        })
        render()
    }

    private fun render() {
        stepsBox.removeAllViews()
        if (steps.isEmpty()) {
            stepsBox.addView(TextView(this).apply { text = "Aucune étape pour l'instant." })
        }
        steps.forEachIndexed { index, step ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(
                TextView(this).apply {
                    text = "${index + 1}. ${step.describe()}"
                    setOnClickListener { stepDialog(step.type, step, index) }
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
            )
            row.addView(smallButton("↑") { move(index, -1) })
            row.addView(smallButton("↓") { move(index, 1) })
            row.addView(smallButton("✕") { steps.removeAt(index); render() })
            stepsBox.addView(row)
        }
    }

    private fun smallButton(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        minWidth = 0
        minimumWidth = 0
        setOnClickListener { onClick() }
    }

    private fun move(index: Int, delta: Int) {
        val target = index + delta
        if (target !in steps.indices) return
        val s = steps.removeAt(index)
        steps.add(target, s)
        render()
    }

    private fun chooseType() {
        val types = StepType.values()
        AlertDialog.Builder(this)
            .setTitle("Ajouter une étape")
            .setItems(types.map { it.label }.toTypedArray()) { _, which -> stepDialog(types[which], null, null) }
            .show()
    }

    private fun stepDialog(type: StepType, existing: Step?, index: Int?) {
        val pad = (16 * resources.displayMetrics.density).toInt()
        val labelField = EditText(this).apply {
            hint = "Nom (ex : Éteindre le salon)"
            setText(existing?.label ?: "")
        }
        val valueField = EditText(this).apply {
            setText(existing?.value ?: "")
            when (type) {
                StepType.ALEXA -> hint = "URL Voice Monkey (https://...)"
                StepType.WEB -> hint = "URL (https://...)"
                StepType.OPEN -> hint = "Lien ou appli (https://..., googlehome://...)"
                StepType.WAIT -> {
                    hint = "Durée en secondes"
                    inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                }
                StepType.MESSAGE -> hint = "Texte à afficher"
            }
        }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, 0)
            if (type == StepType.ALEXA) addView(labelField)
            addView(valueField)
        }
        AlertDialog.Builder(this)
            .setTitle(type.label)
            .setView(form)
            .setPositiveButton("OK") { _, _ ->
                val value = valueField.text.toString().trim()
                val webLike = type == StepType.ALEXA || type == StepType.WEB
                when {
                    value.isEmpty() -> toast("Valeur requise")
                    webLike && !value.startsWith("http") -> toast("L'URL doit commencer par http ou https")
                    type == StepType.WAIT && value.toDoubleOrNull() == null -> toast("Durée invalide")
                    else -> {
                        val step = Step(type, value, labelField.text.toString().trim())
                        if (index == null) steps.add(step) else steps[index] = step
                        render()
                    }
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun save() {
        val name = nameField.text.toString().trim()
        if (name.isEmpty() || steps.isEmpty()) {
            toast("Un nom et au moins une étape sont requis")
            return
        }
        store.save(Shortcut(id ?: java.util.UUID.randomUUID().toString(), name, steps.toList()))
        finish()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
