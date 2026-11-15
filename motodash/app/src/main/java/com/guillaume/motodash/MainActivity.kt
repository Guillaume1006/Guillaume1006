package com.guillaume.motodash

import android.Manifest
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.guillaume.motodash.ui.DashScreen
import com.guillaume.motodash.ui.MotoTheme
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    private lateinit var tracker: LocationTracker
    private val locationGranted = mutableStateOf(false)

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        locationGranted.value = tracker.hasPermission()
        tracker.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().apply {
            load(this@MainActivity, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = packageName
        }
        // Écran toujours allumé + plein écran : on roule, on ne touche pas au téléphone.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        tracker = LocationTracker(this)
        locationGranted.value = tracker.hasPermission()
        val prefs = Prefs(this)

        setContent {
            MotoTheme {
                DashScreen(tracker = tracker, prefs = prefs, locationGranted = locationGranted.value)
            }
        }

        if (!locationGranted.value) {
            permLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    override fun onStart() {
        super.onStart()
        tracker.start()
    }

    override fun onStop() {
        super.onStop()
        tracker.stop()
    }
}
