package com.guillaume.motodash.ui

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guillaume.motodash.Apps
import com.guillaume.motodash.LocationTracker
import com.guillaume.motodash.Prefs
import com.guillaume.motodash.RideState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Widget(val label: String, val icon: ImageVector, val colors: List<Color>) {
    MUSIC("Musique", Icons.Filled.MusicNote, listOf(Color(0xFFFF5A7A), Color(0xFFE0245E))),
    PHONE("Appels", Icons.Filled.Phone, listOf(Color(0xFF5FE07A), Color(0xFF2DB84C))),
    SPEED("Compteur", Icons.Filled.Speed, listOf(Color(0xFFFFA040), Color(0xFFFF6A00))),
    APPS("Applis", Icons.Filled.Apps, listOf(Color(0xFF8E8E9A), Color(0xFF55555F))),
}

@Composable
fun DashScreen(tracker: LocationTracker, prefs: Prefs, locationGranted: Boolean) {
    val ride by tracker.state.collectAsStateWithLifecycle()
    val cfg = LocalConfiguration.current
    val landscape = cfg.screenWidthDp > cfg.screenHeightDp
    var widget by remember { mutableIntStateOf(prefs.tab.coerceIn(0, Widget.entries.size - 1)) }
    var favs by remember { mutableStateOf(prefs.favApps) }
    val current = Widget.entries[widget]
    val select: (Widget) -> Unit = { widget = it.ordinal; prefs.tab = it.ordinal }
    val setFavs: (List<String>) -> Unit = { favs = it; prefs.favApps = it }

    val gap = Arrangement.spacedBy(10.dp)
    Box(Modifier.fillMaxSize().wallpaper().padding(10.dp)) {
        if (landscape) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = gap) {
                Dock(true, ride.hasFix, current, favs, select, Modifier.width(80.dp).fillMaxHeight())
                MapPanel(ride, prefs, locationGranted, Modifier.weight(1.2f).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = gap) {
                    ActionCard(ride, prefs, Modifier.fillMaxWidth().weight(0.34f))
                    WidgetCard(current, tracker, prefs, favs, setFavs, Modifier.fillMaxWidth().weight(0.66f))
                }
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = gap) {
                MapPanel(ride, prefs, locationGranted, Modifier.weight(1f).fillMaxWidth())
                ActionCard(ride, prefs, Modifier.fillMaxWidth().height(116.dp))
                WidgetCard(current, tracker, prefs, favs, setFavs, Modifier.fillMaxWidth().weight(0.85f))
                Dock(false, ride.hasFix, current, favs, select, Modifier.fillMaxWidth().height(80.dp))
            }
        }
    }
}

@Composable
private fun WidgetCard(
    w: Widget, tracker: LocationTracker, prefs: Prefs,
    favs: List<String>, setFavs: (List<String>) -> Unit, modifier: Modifier,
) {
    Box(modifier.glass()) {
        when (w) {
            Widget.MUSIC -> MusicPanel()
            Widget.PHONE -> PhonePanel(prefs)
            Widget.SPEED -> SpeedPanel(tracker)
            Widget.APPS -> AppsPanel(favs, setFavs)
        }
    }
}

// ───────────────────────── Dock ─────────────────────────

@Composable
private fun Dock(
    vertical: Boolean, hasFix: Boolean, selected: Widget, favs: List<String>,
    onSelect: (Widget) -> Unit, modifier: Modifier,
) {
    val ctx = LocalContext.current
    val (time, battery) = rememberClockAndBattery()
    val icons: @Composable () -> Unit = {
        listOf(Widget.MUSIC, Widget.PHONE, Widget.SPEED).forEach { w ->
            DockIcon(w.icon, w.label, Brush.verticalGradient(w.colors), w == selected) { onSelect(w) }
        }
        favs.forEach { pkg ->
            Apps.entry(ctx, pkg)?.let { app ->
                DockApp(app.icon, app.label) { Apps.launch(ctx, pkg) }
            }
        }
    }
    val status: @Composable () -> Unit = {
        Text(time, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (hasFix) Color(0xFF4CD964) else Color(0xFFFF453A)))
            Spacer(Modifier.width(5.dp))
            if (battery >= 0) Text("$battery%", color = if (battery < 20) Color(0xFFFF453A) else TextDim, fontSize = 12.sp)
        }
    }
    val grid: @Composable () -> Unit = {
        DockIcon(Icons.Filled.Apps, "Toutes mes applis", Brush.verticalGradient(Widget.APPS.colors), selected == Widget.APPS) {
            onSelect(Widget.APPS)
        }
    }

    if (vertical) {
        Column(modifier.glass(24.dp).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            status()
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { icons() }
            Spacer(Modifier.height(10.dp))
            grid()
        }
    } else {
        Row(modifier.glass(24.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { status() }
            Spacer(Modifier.width(14.dp))
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { icons() }
            Spacer(Modifier.width(10.dp))
            grid()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockIcon(icon: ImageVector, label: String, brush: Brush, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(15.dp)
    Box(
        Modifier.size(54.dp).clip(shape).background(brush)
            .then(if (selected) Modifier.border(2.5.dp, Color.White, shape) else Modifier)
            .combinedClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = Color.White, modifier = Modifier.size(30.dp)) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockApp(icon: androidx.compose.ui.graphics.ImageBitmap, label: String, onClick: () -> Unit) {
    Image(icon, label, Modifier.size(54.dp).clip(RoundedCornerShape(15.dp)).combinedClickable(onClick = onClick))
}

@Composable
private fun rememberClockAndBattery(): Pair<String, Int> {
    val ctx = LocalContext.current
    var time by remember { mutableStateOf("") }
    var battery by remember { mutableIntStateOf(-1) }
    LaunchedEffect(Unit) {
        val fmt = SimpleDateFormat("HH:mm", Locale.FRANCE)
        while (true) {
            time = fmt.format(Date())
            val b: Intent? = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = b?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = b?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            battery = if (level >= 0) level * 100 / scale else -1
            delay(5_000)
        }
    }
    return time to battery
}

// ───────────────────────── Vitesse + Go / Voix ─────────────────────────

@Composable
private fun ActionCard(ride: RideState, prefs: Prefs, modifier: Modifier) {
    val ctx = LocalContext.current
    var pickNav by remember { mutableStateOf(false) }

    Row(modifier.glass().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${ride.speedKmh.toInt()}", color = Color.White, fontSize = 54.sp, fontWeight = FontWeight.Black, lineHeight = 54.sp)
                Text(" km/h", color = TextDim, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
            Text("max ${ride.maxKmh.toInt()} km/h", color = TextDim, fontSize = 12.sp)
        }
        Spacer(Modifier.weight(1f))
        ActionButton("Go", Icons.Filled.Directions, GoBlue, onLongClick = { pickNav = true }) {
            val nav = prefs.navApp
            if (nav == null) pickNav = true else Apps.launch(ctx, nav)
        }
        Spacer(Modifier.width(18.dp))
        ActionButton("Voix", Icons.Filled.Mic, MicRed) {
            try {
                ctx.startActivity(Intent(Intent.ACTION_VOICE_COMMAND).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) {
                Toast.makeText(ctx, "Aucun assistant vocal trouvé", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (pickNav) {
        AppPickerDialog(
            title = "Appli de navigation (Waze, Maps…)",
            onDismiss = { pickNav = false },
            onPick = { pkg -> prefs.navApp = pkg; pickNav = false; Apps.launch(ctx, pkg) },
        )
    }
}

@Composable
private fun ActionButton(label: String, icon: ImageVector, color: Color, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RoundButton(icon, label, size = 62.dp, background = color, onLongClick = onLongClick, onClick = onClick)
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
