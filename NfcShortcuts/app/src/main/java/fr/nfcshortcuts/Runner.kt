package fr.nfcshortcuts

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object Runner {
    /** Exécute les actions dans l'ordre. Appels web en arrière-plan, liens ouverts via Intent. */
    fun run(context: Context, shortcut: Shortcut) {
        val app = context.applicationContext
        val main = Handler(Looper.getMainLooper())
        thread {
            var ok = 0
            var failed = 0
            for (action in shortcut.actions) {
                val line = action.trim()
                if (line.isEmpty()) continue
                try {
                    if (line.startsWith("http://") || line.startsWith("https://")) {
                        val conn = URL(line).openConnection() as HttpURLConnection
                        conn.connectTimeout = 8000
                        conn.readTimeout = 8000
                        val code = conn.responseCode
                        conn.disconnect()
                        if (code in 200..299) ok++ else failed++
                    } else {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(line)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        main.post { app.startActivity(intent) }
                        ok++
                    }
                } catch (e: Exception) {
                    failed++
                }
            }
            main.post {
                val msg = if (failed == 0) "${shortcut.name} : OK ($ok)" else "${shortcut.name} : $ok OK, $failed en erreur"
                Toast.makeText(app, msg, Toast.LENGTH_LONG).show()
            }
        }
    }
}
