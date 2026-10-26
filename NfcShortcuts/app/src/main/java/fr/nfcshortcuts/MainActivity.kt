package fr.nfcshortcuts

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var store: ShortcutStore
    private lateinit var listView: ListView
    private lateinit var hint: TextView
    private var shortcuts: List<Shortcut> = emptyList()
    private var writeDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = ShortcutStore(this)

        // Lancé par un scan de tag : on exécute et on ferme, sans afficher l'interface.
        if (handleTagIntent(intent)) {
            finish()
            return
        }

        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        val add = Button(this).apply {
            text = "+ Nouveau raccourci"
            setOnClickListener { editDialog(null) }
        }
        hint = TextView(this).apply {
            text = "Aucun raccourci. Créez-en un, puis écrivez-le sur un tag NFC."
            setPadding(0, pad, 0, pad)
        }
        listView = ListView(this)
        listView.setOnItemClickListener { _, _, position, _ -> actionsDialog(shortcuts[position]) }
        root.addView(add)
        root.addView(hint)
        root.addView(listView)
        setContentView(root)
        refresh()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleTagIntent(intent)
    }

    override fun onPause() {
        super.onPause()
        stopWriting()
    }

    private fun handleTagIntent(intent: Intent?): Boolean {
        val data = intent?.data ?: return false
        if (data.scheme != "nfcshortcut") return false
        val shortcut = data.lastPathSegment?.let { store.find(it) }
        if (shortcut == null) {
            Toast.makeText(this, "Raccourci inconnu sur ce téléphone", Toast.LENGTH_LONG).show()
        } else {
            Runner.run(this, shortcut)
        }
        return true
    }

    private fun refresh() {
        shortcuts = store.all().sortedBy { it.name.lowercase() }
        listView.adapter = ArrayAdapter(
            this, android.R.layout.simple_list_item_1,
            shortcuts.map { "${it.name}  (${it.actions.size} action${if (it.actions.size > 1) "s" else ""})" },
        )
        hint.visibility = if (shortcuts.isEmpty()) TextView.VISIBLE else TextView.GONE
    }

    private fun actionsDialog(s: Shortcut) {
        AlertDialog.Builder(this)
            .setTitle(s.name)
            .setItems(arrayOf("Tester", "Écrire sur un tag NFC", "Modifier", "Supprimer")) { _, which ->
                when (which) {
                    0 -> Runner.run(this, s)
                    1 -> startWriting(s)
                    2 -> editDialog(s)
                    3 -> AlertDialog.Builder(this)
                        .setMessage("Supprimer « ${s.name} » ?")
                        .setPositiveButton("Supprimer") { _, _ -> store.delete(s.id); refresh() }
                        .setNegativeButton("Annuler", null)
                        .show()
                }
            }
            .show()
    }

    private fun editDialog(existing: Shortcut?) {
        val pad = (16 * resources.displayMetrics.density).toInt()
        val name = EditText(this).apply {
            hint = "Nom (ex : Départ maison)"
            setText(existing?.name ?: "")
        }
        val actions = EditText(this).apply {
            hint = "Une action par ligne :\nhttps://api.voicemonkey.io/trigger?... \nhttps://home.exemple/webhook/xyz\nhttps://...  ou  un lien d'appli"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 4
            setText(existing?.actions?.joinToString("\n") ?: "")
        }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, 0)
            addView(name)
            addView(actions)
        }
        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Nouveau raccourci" else "Modifier")
            .setView(form)
            .setPositiveButton("Enregistrer") { _, _ ->
                val n = name.text.toString().trim()
                val lines = actions.text.toString().lines().map { it.trim() }.filter { it.isNotEmpty() }
                if (n.isEmpty() || lines.isEmpty()) {
                    Toast.makeText(this, "Nom et au moins une action requis", Toast.LENGTH_LONG).show()
                } else {
                    store.save(Shortcut(existing?.id ?: java.util.UUID.randomUUID().toString(), n, lines))
                    refresh()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun startWriting(s: Shortcut) {
        val adapter = NfcAdapter.getDefaultAdapter(this)
        if (adapter == null) {
            Toast.makeText(this, "Pas de NFC sur ce téléphone", Toast.LENGTH_LONG).show()
            return
        }
        if (!adapter.isEnabled) {
            Toast.makeText(this, "Activez le NFC", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
            return
        }
        writeDialog = AlertDialog.Builder(this)
            .setTitle("Écrire « ${s.name} »")
            .setMessage("Approchez le tag NFC de l'arrière du téléphone.")
            .setNegativeButton("Annuler") { _, _ -> stopWriting() }
            .setOnCancelListener { stopWriting() }
            .show()
        adapter.enableReaderMode(
            this,
            { tag -> writeTag(tag, s) },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null,
        )
    }

    private fun stopWriting() {
        NfcAdapter.getDefaultAdapter(this)?.disableReaderMode(this)
        writeDialog?.dismiss()
        writeDialog = null
    }

    private fun writeTag(tag: Tag, s: Shortcut) {
        val message = NdefMessage(
            arrayOf(
                NdefRecord.createUri("nfcshortcut://run/${s.id}"),
                // Garantit que ce tag ouvre cette appli
                NdefRecord.createApplicationRecord(packageName),
            ),
        )
        val result = try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                try {
                    when {
                        !ndef.isWritable -> "Tag en lecture seule"
                        ndef.maxSize < message.byteArrayLength -> "Tag trop petit"
                        else -> { ndef.writeNdefMessage(message); null }
                    }
                } finally {
                    ndef.close()
                }
            } else {
                val fmt = NdefFormatable.get(tag)
                if (fmt == null) {
                    "Tag non compatible"
                } else {
                    fmt.connect()
                    try { fmt.format(message); null } finally { fmt.close() }
                }
            }
        } catch (e: Exception) {
            "Échec d'écriture : ${e.message ?: e.javaClass.simpleName}"
        }
        runOnUiThread {
            stopWriting()
            Toast.makeText(this, result ?: "Tag écrit ✔ Scannez-le pour tester", Toast.LENGTH_LONG).show()
        }
    }
}
