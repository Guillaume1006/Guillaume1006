package com.guillaume.motodash.ui

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.RampLeft
import androidx.compose.material.icons.filled.RampRight
import androidx.compose.material.icons.filled.RoundaboutRight
import androidx.compose.material.icons.filled.Straight
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.TurnSharpLeft
import androidx.compose.material.icons.filled.TurnSharpRight
import androidx.compose.material.icons.filled.TurnSlightLeft
import androidx.compose.material.icons.filled.TurnSlightRight
import androidx.compose.material.icons.filled.UTurnLeft
import androidx.compose.material.icons.filled.UTurnRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guillaume.motodash.nav.CameraAlert
import com.guillaume.motodash.nav.NavState
import com.guillaume.motodash.nav.Navigator
import com.guillaume.motodash.nav.Status
import com.guillaume.motodash.nav.formatDistance
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.OnCameraTrackingChangedListener
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Carte OpenStreetMap gratuite (OpenFreeMap), sans compte ni clé. */
private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

/** La carte de navigation, dans le grand cadre arrondi. */
@Composable
fun MapCard(nav: Navigator, location: Location?, northUp: Boolean, onSearch: () -> Unit, modifier: Modifier) {
    val ctx = LocalContext.current
    val s by nav.state.collectAsStateWithLifecycle()
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    var tracking by remember { mutableStateOf(true) }

    val mapView = remember {
        MapLibre.getInstance(ctx)
        MapView(ctx).apply {
            onCreate(null)
            getMapAsync { m ->
                m.uiSettings.isCompassEnabled = false
                m.uiSettings.isLogoEnabled = false
                m.uiSettings.setAttributionMargins(16, 0, 0, 16)
                m.cameraPosition = CameraPosition.Builder().target(LatLng(46.6, 2.4)).zoom(5.0).build()
                m.setStyle(Style.Builder().fromUri(STYLE_URL)) { st ->
                    addRouteLayers(st)
                    enableLocation(ctx, m, st) { tracking = false }
                    style = st
                }
                map = m
            }
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> {}
            }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs); mapView.onDestroy() }
    }

    // Position GPS → flèche sur la carte
    LaunchedEffect(location, style) {
        val m = map ?: return@LaunchedEffect
        if (style == null || location == null) return@LaunchedEffect
        runCatching { m.locationComponent.forceLocationUpdate(location) }
        style?.getSourceAs<GeoJsonSource>("me")?.setGeoJson(Point.fromLngLat(location.longitude, location.latitude))
    }
    // Itinéraire → ligne bleue + point d'arrivée
    LaunchedEffect(s.route, s.destination, style) {
        val st = style ?: return@LaunchedEffect
        val r = s.route
        st.getSourceAs<GeoJsonSource>("route")?.setGeoJson(
            if (r == null) FeatureCollection.fromFeatures(emptyList())
            else FeatureCollection.fromFeature(Feature.fromGeometry(LineString.fromLngLats(r.points.map { Point.fromLngLat(it.lon, it.lat) })))
        )
        val d = s.destination
        st.getSourceAs<GeoJsonSource>("dest")?.setGeoJson(
            if (d == null) FeatureCollection.fromFeatures(emptyList())
            else FeatureCollection.fromFeature(Feature.fromGeometry(Point.fromLngLat(d.lon, d.lat)))
        )
    }
    // Radars sur la carte : Belgique uniquement (en France, la loi interdit de les situer)
    LaunchedEffect(s.cameras, style) {
        val st = style ?: return@LaunchedEffect
        st.getSourceAs<GeoJsonSource>("cams")?.setGeoJson(
            FeatureCollection.fromFeatures(s.cameras.filter { !it.cam.france }.map { Feature.fromGeometry(Point.fromLngLat(it.cam.lon, it.cam.lat)) })
        )
    }
    // Caméra : vue de conduite inclinée pendant le guidage, vue du dessus sinon
    LaunchedEffect(s.status, tracking, style, location != null, northUp) {
        val m = map ?: return@LaunchedEffect
        if (style == null || location == null) return@LaunchedEffect
        val lc = m.locationComponent
        if (!lc.isLocationComponentActivated) return@LaunchedEffect
        val navigating = s.status == Status.NAVIGATING
        lc.renderMode = if (navigating) RenderMode.GPS else RenderMode.COMPASS
        if (tracking) {
            lc.cameraMode = when {
                navigating && !northUp -> CameraMode.TRACKING_GPS        // la carte tourne avec la moto
                navigating -> CameraMode.TRACKING_GPS_NORTH               // nord en haut
                else -> CameraMode.TRACKING
            }
            lc.zoomWhileTracking(if (navigating) 16.5 else 15.0)
            lc.tiltWhileTracking(if (navigating) 50.0 else 0.0)
        }
    }

    Box(modifier.clip(RoundedCornerShape(26.dp)).background(Card)) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        // Haut : prochaine manœuvre / état
        Column(Modifier.align(Alignment.TopStart).padding(12.dp)) {
            when (s.status) {
                Status.NAVIGATING -> {
                    ManeuverBanner(s)
                    s.alert?.let { Spacer(Modifier.size(8.dp)); AlertChip(it, location) }
                }
                Status.ROUTING -> Chip("Calcul de l'itinéraire…", loading = true)
                Status.ARRIVED -> Chip("Vous êtes arrivé 🏁") { nav.stop() }
                Status.ERROR -> Chip(s.error ?: "Erreur", color = MicRed) { nav.stop() }
                Status.IDLE -> {}
            }
        }

        // Droite : recentrer
        if (!tracking) {
            RoundButton(
                Icons.Filled.MyLocation, "Recentrer", modifier = Modifier.align(Alignment.CenterEnd).padding(12.dp),
                size = 60.dp, background = Accent, tint = Color.Black,
            ) { tracking = true }
        }

        // Bas : vitesse + arrivée, ou « Où va-t-on ? »
        Row(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SpeedBubble(location, (s.alert as? CameraAlert.Radar)?.limit)
            if (s.status == Status.NAVIGATING || s.status == Status.ROUTING) {
                EtaBar(s, Modifier.weight(1f)) { nav.stop() }
            } else {
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.clip(RoundedCornerShape(30.dp)).background(GoBlue).clickable(onClick = onSearch)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Search, null, tint = Color.White, modifier = Modifier.size(26.dp))
                    Text("  Où va-t-on ?", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

private fun addRouteLayers(st: Style) {
    // Pas de bâtiments en 3D : inutiles à moto et plus gourmands pour le téléphone
    st.layers.filterIsInstance<org.maplibre.android.style.layers.FillExtrusionLayer>().forEach { st.removeLayer(it) }
    st.addSource(GeoJsonSource("route"))
    st.addSource(GeoJsonSource("dest"))
    st.addLayer(LineLayer("route-casing", "route").withProperties(
        PropertyFactory.lineColor("#1557C0"), PropertyFactory.lineWidth(13f),
        PropertyFactory.lineCap(Property.LINE_CAP_ROUND), PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
    ))
    st.addLayer(LineLayer("route-line", "route").withProperties(
        PropertyFactory.lineColor("#3D8BFF"), PropertyFactory.lineWidth(8f),
        PropertyFactory.lineCap(Property.LINE_CAP_ROUND), PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
    ))
    st.addSource(GeoJsonSource("cams"))
    st.addLayer(CircleLayer("cams-point", "cams").withProperties(
        PropertyFactory.circleColor("#FFFFFF"), PropertyFactory.circleRadius(9f),
        PropertyFactory.circleStrokeColor("#E53935"), PropertyFactory.circleStrokeWidth(5f),
    ))
    st.addSource(GeoJsonSource("me"))
    st.addLayer(CircleLayer("me-halo", "me").withProperties(
        PropertyFactory.circleColor("#1A73E8"), PropertyFactory.circleOpacity(0.25f), PropertyFactory.circleRadius(22f),
        PropertyFactory.circlePitchAlignment(Property.CIRCLE_PITCH_ALIGNMENT_MAP),
    ))
    st.addLayer(CircleLayer("me-dot", "me").withProperties(
        PropertyFactory.circleColor("#FF7A00"), PropertyFactory.circleRadius(11f),
        PropertyFactory.circleStrokeColor("#FFFFFF"), PropertyFactory.circleStrokeWidth(4f),
        PropertyFactory.circlePitchAlignment(Property.CIRCLE_PITCH_ALIGNMENT_MAP),
    ))
    st.addLayer(CircleLayer("dest-point", "dest").withProperties(
        PropertyFactory.circleColor("#E53935"), PropertyFactory.circleRadius(10f),
        PropertyFactory.circleStrokeColor("#FFFFFF"), PropertyFactory.circleStrokeWidth(3f),
    ))
}

@SuppressLint("MissingPermission")
private fun enableLocation(ctx: Context, m: MapLibreMap, st: Style, onDismissed: () -> Unit) {
    runCatching {
        val lc = m.locationComponent
        lc.activateLocationComponent(
            LocationComponentActivationOptions.builder(ctx, st).useDefaultLocationEngine(false).build()
        )
        lc.isLocationComponentEnabled = true
        lc.cameraMode = CameraMode.TRACKING
        lc.renderMode = RenderMode.COMPASS
        lc.addOnCameraTrackingChangedListener(object : OnCameraTrackingChangedListener {
            override fun onCameraTrackingDismissed() = onDismissed()   // la carte a été déplacée au doigt
            override fun onCameraTrackingChanged(currentMode: Int) {}
        })
    }
}

@Composable
private fun ManeuverBanner(s: NavState) {
    val m = s.next ?: return
    Row(
        Modifier.widthIn(max = 520.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xF0101014)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(GoBlue), contentAlignment = Alignment.Center) {
            Icon(maneuverIcon(m.type), null, tint = Color.White, modifier = Modifier.size(46.dp))
            if (m.exitCount != null) Text("${m.exitCount}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp))
        }
        Column(Modifier.padding(start = 14.dp)) {
            Text(if (s.rerouting) "Recalcul…" else formatDistance(s.distToNext), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
            Text(m.street ?: m.instruction, color = Color(0xFF7CC4FF), fontSize = 19.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AlertChip(a: CameraAlert, location: Location?) {
    val (bg, icon, text) = when (a) {
        is CameraAlert.Radar -> Triple(MicRed, Icons.Filled.PhotoCamera,
            "Radar" + (a.limit?.let { " · $it" } ?: "") + " · " + formatDistance(a.distance))
        is CameraAlert.DangerZone -> Triple(Color(0xFFFF9800), Icons.Filled.Warning, "Zone de danger · " + formatDistance(a.remaining))
    }
    Row(
        Modifier.clip(RoundedCornerShape(18.dp)).background(bg).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
        Text("  $text", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EtaBar(s: NavState, modifier: Modifier, onStop: () -> Unit) {
    val arrival = SimpleDateFormat("HH:mm", Locale.FRANCE).format(Date(System.currentTimeMillis() + (s.remainingS * 1000).toLong()))
    val mins = (s.remainingS / 60).toInt()
    val dur = if (mins >= 60) "${mins / 60} h ${"%02d".format(mins % 60)}" else "$mins min"
    Row(
        modifier.clip(RoundedCornerShape(22.dp)).background(Color(0xF0101014)).padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (s.status == Status.ROUTING) {
            Text("Calcul…", color = Color.White, fontSize = 20.sp, modifier = Modifier.weight(1f))
        } else {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Arrivée ", color = Color(0xFF7CC4FF), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(arrival, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
                Text("$dur · ${formatDistance(s.remainingM)}", color = Color.White, fontSize = 17.sp, maxLines = 1)
            }
        }
        RoundButton(Icons.Filled.Close, "Arrêter le guidage", size = 52.dp, background = MicRed, onClick = onStop)
    }
}

@Composable
private fun SpeedBubble(location: Location?, limit: Int?) {
    val kmh = location?.takeIf { it.hasSpeed() }?.let { (it.speed * 3.6f).toInt() } ?: 0
    val over = limit != null && kmh > limit + 3
    Column(
        Modifier.size(84.dp).clip(CircleShape).background(if (over) MicRed else Color(0xF0101014)),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Text("$kmh", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text("km/h", color = TextDim, fontSize = 12.sp)
    }
}

@Composable
private fun Chip(text: String, color: Color = Color(0xF0101014), loading: Boolean = false, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.clip(RoundedCornerShape(20.dp)).background(color)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) { CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(12.dp)) }
        Text(text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        if (onClick != null) { Spacer(Modifier.width(12.dp)); Icon(Icons.Filled.Close, null, tint = Color.White) }
    }
}

/** Pictogramme de la manœuvre (types Valhalla). */
private fun maneuverIcon(type: Int): ImageVector = when (type) {
    9 -> Icons.Filled.TurnSlightRight
    10 -> Icons.Filled.TurnRight
    11 -> Icons.Filled.TurnSharpRight
    12 -> Icons.Filled.UTurnRight
    13 -> Icons.Filled.UTurnLeft
    14 -> Icons.Filled.TurnSharpLeft
    15 -> Icons.Filled.TurnLeft
    16 -> Icons.Filled.TurnSlightLeft
    18, 20, 23 -> Icons.Filled.RampRight
    19, 21, 24 -> Icons.Filled.RampLeft
    25, 37, 38 -> Icons.Filled.Merge
    26, 27 -> Icons.Filled.RoundaboutRight
    4, 5, 6 -> Icons.Filled.Flag
    1, 2, 3, 7, 8, 17, 22 -> Icons.Filled.Straight
    else -> Icons.Filled.Navigation
}
