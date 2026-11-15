package com.guillaume.motodash

import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.guillaume.motodash.ui.DashScreen
import com.guillaume.motodash.ui.MotoTheme

class MainActivity : ComponentActivity() {
    /** true quand MotoDash est dans une moitié de l'écran partagé, à côté du GPS. */
    private val split = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val prefs = Prefs(this)
        // "panel" force l'affichage en panneau (aperçus / captures d'écran)
        split.value = isInMultiWindowMode || intent.getBooleanExtra("panel", false)

        setContent {
            MotoTheme {
                DashScreen(prefs = prefs, split = split.value)
            }
        }

        // Au lancement : ouvre directement le GPS choisi à côté de MotoDash.
        val nav = prefs.navApp
        if (savedInstanceState == null && prefs.autoLaunch && nav != null && !split.value) {
            window.decorView.post { Apps.launch(this, nav, adjacent = true) }
        }
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        split.value = isInMultiWindowMode || intent.getBooleanExtra("panel", false)
    }
}
