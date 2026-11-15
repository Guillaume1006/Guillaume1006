package com.guillaume.motodash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.guillaume.motodash.Prefs
import com.guillaume.motodash.R
import com.guillaume.motodash.RideState
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

private fun carto(name: String, path: String) = XYTileSource(
    name, 0, 20, 256, "@2x.png",
    arrayOf("a", "b", "c", "d").map { "https://$it.basemaps.cartocdn.com/$path/" }.toTypedArray(),
    "© OpenStreetMap © CARTO",
)

/** Styles de carte : Voyager (clair, proche de CarPlay), OpenStreetMap classique, sombre. */
private val STYLES: List<Pair<ITileSource, String>> = listOf(
    carto("CartoVoyager", "rastertiles/voyager") to "© OpenStreetMap © CARTO",
    TileSourceFactory.MAPNIK to "© OpenStreetMap",
    carto("CartoDark", "dark_all") to "© OpenStreetMap © CARTO",
)

@Composable
fun MapPanel(ride: RideState, prefs: Prefs, locationGranted: Boolean, modifier: Modifier) {
    val ctx = LocalContext.current
    var style by remember { mutableIntStateOf(prefs.mapStyle.coerceIn(0, STYLES.size - 1)) }
    var headingUp by remember { mutableStateOf(prefs.headingUp) }

    val mapView = remember {
        MapView(ctx).apply {
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            controller.setZoom(16.5)
        }
    }
    val myLoc = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(ctx), mapView).also {
            val dot = ContextCompat.getDrawable(ctx, R.drawable.ic_nav_dot)!!.toBitmap()
            val arrow = ContextCompat.getDrawable(ctx, R.drawable.ic_nav_arrow)!!.toBitmap()
            it.setDirectionArrow(dot, arrow)
            it.setPersonAnchor(0.5f, 0.5f)
            it.setDirectionAnchor(0.5f, 0.5f)
            it.isDrawAccuracyEnabled = false
            mapView.overlays.add(it)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> { mapView.onResume(); myLoc.enableMyLocation() }
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> myLoc.disableMyLocation()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(obs)
            mapView.onDetach()
        }
    }

    LaunchedEffect(locationGranted) {
        if (locationGranted) {
            myLoc.enableMyLocation()
            myLoc.enableFollowLocation()
        }
    }
    LaunchedEffect(style) { mapView.setTileSource(STYLES[style].first) }
    LaunchedEffect(headingUp, ride.bearing) {
        val b = ride.bearing
        mapView.mapOrientation = if (headingUp && b != null) -b else 0f
    }

    Box(modifier.clip(RoundedCornerShape(26.dp)).background(Card)) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        Column(
            Modifier.align(Alignment.TopEnd).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RoundButton(Icons.Filled.Layers, "Style de carte", size = 48.dp) {
                style = (style + 1) % STYLES.size; prefs.mapStyle = style
            }
            RoundButton(Icons.Filled.Explore, "Carte orientée vers le cap", size = 48.dp, active = headingUp) {
                headingUp = !headingUp; prefs.headingUp = headingUp
            }
        }

        Column(
            Modifier.align(Alignment.BottomEnd).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RoundButton(Icons.Filled.Add, "Zoom +") { mapView.controller.zoomIn() }
            RoundButton(Icons.Filled.Remove, "Zoom -") { mapView.controller.zoomOut() }
            RoundButton(Icons.Filled.MyLocation, "Recentrer", background = Accent, tint = Color.Black) {
                myLoc.enableFollowLocation()
                mapView.controller.setZoom(16.5)
            }
        }

        Text(
            STYLES[style].second, color = Color(0xFF333333), fontSize = 9.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp),
        )
    }
}
