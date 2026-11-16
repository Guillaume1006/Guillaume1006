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
import com.guillaume.motodash.ui.DashState
import com.guillaume.motodash.ui.Drive
import com.guillaume.motodash.ui.MotoTheme

class MainActivity : ComponentActivity() {
    /** true quand MotoDash est dans une moitié de l'écran partagé, à côté du GPS. */
    private val split = mutableStateOf(false)
    private lateinit var prefs: Prefs
    private lateinit var state: DashState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        prefs = Prefs(this)
        state = DashState(prefs)
        // "panel" force l'affichage en panneau (aperçus / captures d'écran)
        split.value = isInMultiWindowMode || intent.getBooleanExtra("panel", false)

        setContent {
            MotoTheme { DashScreen(state = state, prefs = prefs, split = split.value) }
        }


        // Au lancement : ouvre directement le GPS choisi en écran partagé, à côté de MotoDash.
        if (savedInstanceState == null && prefs.autoLaunch && prefs.navApp != null && !split.value) {
            window.decorView.post { Drive.start(this, state) }
        }
    }

    override fun onResume() {
        super.onResume()
        state.reload()
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        split.value = isInMultiWindowMode || intent.getBooleanExtra("panel", false)
    }
}
