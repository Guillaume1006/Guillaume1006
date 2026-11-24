package fr.cockpit.auto.ui

import android.annotation.SuppressLint
import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import fr.cockpit.auto.apps.Launchers
import fr.cockpit.auto.data.NextEvent
import fr.cockpit.auto.data.Weather
import fr.cockpit.auto.data.fetchWeather
import fr.cockpit.auto.data.hasLocation
import fr.cockpit.auto.data.lastLocation
import fr.cockpit.auto.data.nextEvent
import fr.cockpit.auto.media.NowPlaying
import fr.cockpit.auto.media.Track
import kotlinx.coroutines.delay
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Carte sombre gratuite, sans clé (données OpenStreetMap). */
private const val MAP_STYLE = "https://tiles.openfreemap.org/styles/dark"
private val PARIS = LatLng(48.8566, 2.3522)

@Composable
fun Dashboard(nowPlaying: NowPlaying, refresh: Int, onOpenMusic: () -> Unit) {
    val context = LocalContext.current
    val located = remember(refresh) { context.hasLocation() }
    val overlayOk = remember(refresh) { android.provider.Settings.canDrawOverlays(context) }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        key(located) {
            MapCard(
                located = located,
                overlayOk = overlayOk,
                onOpen = { Launchers.openNavigation(context) },
                modifier = Modifier.weight(1.6f).fillMaxHeight(),
            )
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NowPlayingCard(nowPlaying, onOpenMusic, Modifier.weight(1.15f).fillMaxWidth())
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AgendaTile(refresh, Modifier.weight(1f).fillMaxHeight())
                WeatherTile(refresh, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

/* ---------- Carte ---------- */

/** La carte (bibliothèque native) ne tourne pas dans les tests de capture d'écran. */
private val IS_ROBOLECTRIC = android.os.Build.FINGERPRINT == "robolectric"

@SuppressLint("MissingPermission")
@Composable
private fun MapCard(located: Boolean, overlayOk: Boolean, onOpen: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val mapView = remember {
        if (IS_ROBOLECTRIC) return@remember null
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    if (mapView != null) DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
        }
    }

    KCard(modifier) {
        if (IS_ROBOLECTRIC) {
            Box(Modifier.fillMaxSize().background(Color(0xFF121A22)))
        } else AndroidView(
            factory = {
                mapView!!.getMapAsync { map ->
                    map.uiSettings.isLogoEnabled = false
                    map.uiSettings.isCompassEnabled = false
                    map.addOnMapClickListener { onOpen(); true }
                    map.setStyle(Style.Builder().fromUri(MAP_STYLE)) { style ->
                        if (located) {
                            val lc = map.locationComponent
                            lc.activateLocationComponent(
                                LocationComponentActivationOptions.builder(context, style)
                                    .useDefaultLocationEngine(true)
                                    .build()
                            )
                            lc.isLocationComponentEnabled = true
                            lc.renderMode = RenderMode.GPS
                            lc.cameraMode = CameraMode.TRACKING_GPS
                            lc.zoomWhileTracking(15.5)
                        } else {
                            map.cameraPosition = CameraPosition.Builder().target(PARIS).zoom(11.0).build()
                        }
                    }
                }
                mapView
            },
            modifier = Modifier.fillMaxSize(),
        )
        // Le guidage est confié à Waze ; la barre flottante reste visible par-dessus.
        Row(
            Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(K.accent)
                .clickable(onClick = onOpen)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Navigation, null, tint = K.accentInk, modifier = Modifier.size(24.dp))
            Spacer(Modifier.size(10.dp))
            Text("Ouvrir Waze", color = K.accentInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Column(
            Modifier.align(Alignment.BottomStart).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!overlayOk) Chip("Garder la barre visible dans Waze : autoriser") {
                Launchers.openOverlayPermission(context)
            }
            if (!located) Chip("Position non autorisée") { Launchers.openAppSettings(context) }
        }
    }
}

@Composable
private fun Chip(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = K.text,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(K.bg.copy(alpha = 0.88f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

/* ---------- Lecture en cours ---------- */

@Composable
private fun NowPlayingCard(nowPlaying: NowPlaying, onOpenMusic: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val access by nowPlaying.hasAccess.collectAsState()
    val track by nowPlaying.track.collectAsState()

    KCard(modifier, onClick = onOpenMusic) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            val t = track
            when {
                !access -> Prompt(
                    title = "Musique",
                    text = "Autorise l'accès aux notifications pour piloter Spotify, Deezer, YouTube Music…",
                    action = "Autoriser",
                    onAction = { Launchers.openNotificationAccess(context) },
                )
                t == null -> Prompt(
                    title = "Musique",
                    text = "Rien en lecture.",
                    action = "Ouvrir la musique",
                    onAction = onOpenMusic,
                )
                else -> PlayingContent(t, nowPlaying)
            }
        }
    }
}

@Composable
private fun ColumnScope.PlayingContent(t: Track, nowPlaying: NowPlaying) {
    // Fait avancer la barre de progression pendant la lecture
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(t.playing, t.updatedAt) {
        while (t.playing) {
            tick = SystemClock.elapsedRealtime()
            delay(500)
        }
    }
    val progress = remember(tick, t) {
        if (t.durationMs > 0) t.currentPositionMs().toFloat() / t.durationMs else 0f
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(76.dp).clip(RoundedCornerShape(12.dp)).background(Tile.art)) {
            t.art?.let { bmp ->
                Image(bmp.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            KLabel(t.appLabel)
            Text(t.title, color = K.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(t.artist, color = K.dim, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    Spacer(Modifier.weight(1f))
    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(K.line)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(K.text))
    }
    Spacer(Modifier.weight(1f))
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlButton(Icons.Rounded.SkipPrevious, "Précédent") { nowPlaying.previous() }
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(K.text).clickable { nowPlaying.playPause() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (t.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                if (t.playing) "Pause" else "Lecture",
                tint = K.bg,
                modifier = Modifier.size(30.dp),
            )
        }
        ControlButton(Icons.Rounded.SkipNext, "Suivant") { nowPlaying.next() }
    }
}

@Composable
private fun ControlButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(Modifier.size(52.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = K.text, modifier = Modifier.size(34.dp))
    }
}

@Composable
private fun ColumnScope.Prompt(title: String, text: String, action: String, onAction: () -> Unit) {
    KLabel(title)
    VSpace(8)
    Text(text, color = K.text, fontSize = 15.sp, lineHeight = 20.sp)
    Spacer(Modifier.weight(1f))
    Text(
        action,
        color = K.accentInk,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(K.accent)
            .clickable(onClick = onAction)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

/* ---------- Agenda ---------- */

@Composable
private fun AgendaTile(refresh: Int, modifier: Modifier) {
    val context = LocalContext.current
    var event by remember { mutableStateOf<NextEvent?>(null) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(refresh) {
        while (true) {
            event = context.nextEvent()
            loaded = true
            delay(5 * 60_000L)
        }
    }
    val now = rememberTicker(60_000)
    val e = event
    MiniTile(
        brush = Tile.cal,
        icon = Icons.Rounded.Event,
        big = e?.let { formatTime(it.beginMs) } ?: "–",
        small = when {
            e != null -> listOfNotNull(e.title, untilText(e.beginMs, now)).joinToString("\n")
            loaded -> "Rien de prévu\ndans les 24 h"
            else -> "Agenda"
        },
        onClick = { Launchers.openCalendar(context) },
        modifier = modifier,
    )
}

private fun formatTime(ms: Long): String {
    val dt = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    val hm = dt.format(DateTimeFormatter.ofPattern("HH:mm"))
    return if (dt.toLocalDate() == LocalDate.now()) hm else "dem. $hm"
}

private fun untilText(beginMs: Long, nowMs: Long): String {
    val min = (beginMs - nowMs) / 60_000
    return when {
        min <= 0 -> "maintenant"
        min < 60 -> "dans $min min"
        else -> "dans ${min / 60} h ${"%02d".format(min % 60)}"
    }
}

/* ---------- Météo ---------- */

@Composable
private fun WeatherTile(refresh: Int, modifier: Modifier) {
    val context = LocalContext.current
    var weather by remember { mutableStateOf<Weather?>(null) }
    LaunchedEffect(refresh) {
        while (true) {
            val loc = context.lastLocation()
            fetchWeather(loc?.latitude ?: PARIS.latitude, loc?.longitude ?: PARIS.longitude)?.let { weather = it }
            delay(30 * 60_000L)
        }
    }
    val w = weather
    MiniTile(
        brush = Tile.weather,
        icon = Icons.Rounded.Cloud,
        big = w?.let { "${it.temp}°" } ?: "–",
        small = w?.let {
            val rain = it.rainChance?.takeIf { p -> p >= 20 }?.let { p -> " · pluie $p %" } ?: ""
            "${it.label}\n${it.min}° / ${it.max}°$rain"
        } ?: "Météo",
        onClick = null,
        modifier = modifier,
    )
}

@Composable
private fun MiniTile(
    brush: Brush,
    icon: ImageVector,
    big: String,
    small: String,
    onClick: (() -> Unit)?,
    modifier: Modifier,
) {
    KCard(modifier, onClick = onClick) {
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(brush), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(big, color = K.text, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(small, color = K.dim, fontSize = 13.sp, lineHeight = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
