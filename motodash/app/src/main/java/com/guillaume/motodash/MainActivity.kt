package com.guillaume.motodash

import android.Manifest
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.guillaume.motodash.nav.Navigator
import com.guillaume.motodash.nav.Place
import com.guillaume.motodash.nav.Search
import com.guillaume.motodash.nav.Status
import com.guillaume.motodash.ui.DashScreen
import com.guillaume.motodash.ui.DashState
import com.guillaume.motodash.ui.MotoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var prefs: Prefs
    private lateinit var state: DashState
    private lateinit var nav: Navigator
    private lateinit var tracker: LocationTracker
    private val location = mutableStateOf<Location?>(null)

    private val locationPerm = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        tracker.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Écran toujours allumé, plein écran : on roule, on ne touche pas au téléphone
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        prefs = Prefs(this)
        state = DashState(prefs)
        nav = Navigator(this, prefs, lifecycleScope)
        tracker = LocationTracker(this) { loc ->
            location.value = loc
            nav.onLocation(loc)
        }

        setContent {
            MotoTheme { DashScreen(state, prefs, nav, location.value) }
        }

        if (!tracker.hasPermission()) {
            locationPerm.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        if (savedInstanceState == null) handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** Adresse partagée par une autre appli (« geo:… ») : on lance le guidage. */
    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (intent.action != Intent.ACTION_VIEW || uri.scheme != "geo") return
        val (lat, lon, label) = parseGeo(uri)
        if (lat != null && lon != null && !(lat == 0.0 && lon == 0.0)) {
            nav.start(Place(label ?: "Destination", "", lat, lon))
        } else if (!label.isNullOrBlank()) {
            lifecycleScope.launch {
                val found = runCatching { withContext(Dispatchers.IO) { Search.search(label, nav.position) } }.getOrNull()?.firstOrNull()
                found?.let { nav.start(it) }
            }
        }
    }

    private fun parseGeo(uri: Uri): Triple<Double?, Double?, String?> {
        // geo:lat,lon  |  geo:0,0?q=adresse  |  geo:0,0?q=lat,lon(Nom)
        val ssp = uri.schemeSpecificPart ?: ""
        val coords = ssp.substringBefore('?').split(',')
        var lat = coords.getOrNull(0)?.toDoubleOrNull()
        var lon = coords.getOrNull(1)?.toDoubleOrNull()
        var q = ssp.substringAfter("q=", "").substringBefore('&').let { Uri.decode(it) }.ifBlank { null }
        var label: String? = null
        if (q != null) {
            val m = Regex("""^\s*(-?\d+(\.\d+)?)\s*,\s*(-?\d+(\.\d+)?)\s*(\((.*)\))?""").find(q)
            if (m != null) { lat = m.groupValues[1].toDouble(); lon = m.groupValues[3].toDouble(); label = m.groupValues[6].ifBlank { null }; q = null }
        }
        return Triple(lat, lon, label ?: q)
    }

    override fun onStart() { super.onStart(); tracker.start() }

    override fun onResume() { super.onResume(); state.reload() }

    override fun onStop() {
        super.onStop()
        // Le GPS reste actif pendant un guidage (écran verrouillé, autre appli au premier plan)
        if (nav.state.value.status != Status.NAVIGATING) tracker.stop()
    }

    override fun onDestroy() {
        tracker.stop()
        nav.shutdown()
        super.onDestroy()
    }
}
