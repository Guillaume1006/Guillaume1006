package fr.cockpit.auto.apps

import android.app.ActivityOptions
import android.os.Build
import org.lsposed.hiddenapibypass.HiddenApiBypass

/**
 * Ouvrir une app en fenêtre libre (à une taille et une place données).
 * Android le réserve aux téléphones où « Activer les fenêtres de forme libre » est coché
 * dans les options pour les développeurs ; ailleurs, l'app s'ouvre simplement en plein écran.
 */
object FreeformWindow {
    /** WindowConfiguration.WINDOWING_MODE_FREEFORM */
    private const val WINDOWING_MODE_FREEFORM = 5

    fun request(options: ActivityOptions) {
        if (Build.FINGERPRINT == "robolectric") return // fonction système absente des tests
        runCatching {
            HiddenApiBypass.invoke(ActivityOptions::class.java, options, "setLaunchWindowingMode", WINDOWING_MODE_FREEFORM)
        }
    }
}
