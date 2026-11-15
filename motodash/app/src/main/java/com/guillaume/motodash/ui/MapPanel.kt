package com.guillaume.motodash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.guillaume.motodash.Apps
import com.guillaume.motodash.Prefs
import com.guillaume.motodash.RideState
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun MapPanel(ride: RideState, prefs: Prefs, locationGranted: Boolean, modifier: Modifier) {
    val ctx = LocalContext.current
    var night by remember { mutableStateOf(prefs.nightMap) }
    var headingUp by remember { mutableStateOf(prefs.headingUp) }
    var pickNav by remember { mutableStateOf(false) }

    val mapView = remember {
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            controller.setZoom(16.5)
        }
    }
    val myLoc = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(ctx), mapView).also {
            it.isDrawAccuracyEnabled = true
            mapView.overlays.add(it)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> { mapView.onResume(); myLoc.enableMyLocation() }
                Lifecycle.Event.ON_PAUSE -> { mapView.onPause() }
                Lifecycle.Event.ON_STOP -> { myLoc.disableMyLocation() }
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
    LaunchedEffect(night) {
        mapView.overlayManager.tilesOverlay.setColorFilter(if (night) TilesOverlay.INVERT_COLORS else null)
    }
    LaunchedEffect(headingUp, ride.bearing) {
        val b = ride.bearing
        mapView.mapOrientation = if (headingUp && b != null) -b else 0f
    }

    Box(modifier.clip(RoundedCornerShape(20.dp)).background(Card)) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        // Vitesse en surimpression
        Column(
            Modifier.align(Alignment.TopStart).padding(10.dp)
                .clip(RoundedCornerShape(16.dp)).background(Bg.copy(alpha = 0.85f))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("${ride.speedKmh.toInt()}", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black)
            Text("km/h", color = TextDim, fontSize = 12.sp)
        }

        Column(
            Modifier.align(Alignment.CenterEnd).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RoundButton(Icons.Filled.Add, "Zoom +") { mapView.controller.zoomIn() }
            RoundButton(Icons.Filled.Remove, "Zoom -") { mapView.controller.zoomOut() }
            RoundButton(Icons.Filled.MyLocation, "Recentrer") {
                myLoc.enableFollowLocation()
                mapView.controller.setZoom(16.5)
            }
        }

        Row(
            Modifier.align(Alignment.BottomStart).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RoundButton(Icons.Filled.Explore, "Carte orientée vers le cap", active = headingUp) {
                headingUp = !headingUp; prefs.headingUp = headingUp
            }
            RoundButton(Icons.Filled.DarkMode, "Mode nuit", active = night) {
                night = !night; prefs.nightMap = night
            }
        }

        // Navigation guidée : ouvre l'appli GPS choisie (Waze, Google Maps, Calimoto…)
        RoundButton(
            Icons.Filled.Navigation, "Navigation (appui long : changer d'appli)",
            modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
            size = 72.dp, background = Accent.copy(alpha = 0.95f),
            onLongClick = { pickNav = true },
        ) {
            val nav = prefs.navApp
            if (nav == null) pickNav = true else Apps.launch(ctx, nav)
        }
    }

    if (pickNav) {
        AppPickerDialog(
            title = "Appli de navigation",
            onDismiss = { pickNav = false },
            onPick = { pkg -> prefs.navApp = pkg; pickNav = false; Apps.launch(ctx, pkg) },
        )
    }
}
