package fr.cockpit.auto

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import fr.cockpit.auto.data.CockpitState
import fr.cockpit.auto.overlay.RailOverlayService
import fr.cockpit.auto.ui.CockpitApp
import fr.cockpit.auto.ui.CockpitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Écran toujours allumé et plein écran : le téléphone est sur son support.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()
        handleIntent(intent)
        setContent {
            CockpitTheme {
                CockpitApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        CockpitState.activityVisible.value = true
        // La barre flottante prendra le relais quand une autre app passera devant.
        RailOverlayService.startIfAllowed(this)
    }

    override fun onStop() {
        CockpitState.activityVisible.value = false
        super.onStop()
    }

    /** Au retour d'une autre app, Android réaffiche parfois les barres système : on les recache. */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun handleIntent(intent: Intent?) {
        intent?.getStringExtra(EXTRA_SCREEN)?.let { CockpitState.requestedScreen.value = it }
    }

    companion object {
        const val EXTRA_SCREEN = "screen"
    }
}
