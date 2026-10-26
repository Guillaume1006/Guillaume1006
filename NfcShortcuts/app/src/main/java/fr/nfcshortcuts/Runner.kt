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
    /** Exécute les étapes dans l'ordre, en arrière-plan. Une étape en erreur n'arrête pas les suivantes. */
    fun run(context: Context, shortcut: Shortcut) {
        val app = context.applicationContext
        val main = Handler(Looper.getMainLooper())
        thread {
            var ok = 0
            val errors = mutableListOf<String>()
            for (step in shortcut.steps) {
                try {
                    when (step.type) {
                        StepType.ALEXA, StepType.WEB -> {
                            val conn = URL(step.value.trim()).openConnection() as HttpURLConnection
                            conn.connectTimeout = 8000
                            conn.readTimeout = 8000
                            val code = conn.responseCode
                            conn.disconnect()
                            if (code in 200..299) ok++ else errors += "${step.describe()} (HTTP $code)"
                        }
                        StepType.OPEN -> {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(step.value.trim()))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            main.post { app.startActivity(intent) }
                            ok++
                        }
                        StepType.WAIT -> {
                            Thread.sleep((step.value.trim().toDoubleOrNull() ?: 0.0).times(1000).toLong())
                            ok++
                        }
                        StepType.MESSAGE -> {
                            main.post { Toast.makeText(app, step.value, Toast.LENGTH_SHORT).show() }
                            ok++
                        }
                    }
                } catch (e: Exception) {
                    errors += step.describe()
                }
            }
            main.post {
                val msg = if (errors.isEmpty()) "${shortcut.name} : OK ($ok)"
                else "${shortcut.name} : $ok OK, erreur sur : ${errors.joinToString(", ")}"
                Toast.makeText(app, msg, Toast.LENGTH_LONG).show()
            }
        }
    }
}
