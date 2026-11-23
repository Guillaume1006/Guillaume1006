package com.guillaume.motodash.ui

import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.BatteryManager
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guillaume.motodash.Apps
import com.guillaume.motodash.Prefs
import com.guillaume.motodash.nav.Navigator
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Widget { MUSIC, SEARCH, PHONE, APPS, SETTINGS }

/** État partagé de l'écran : applis choisies, réglages, widget affiché. */
class DashState(private val prefs: Prefs) {
    var widget by mutableStateOf(Widget.entries.getOrElse(prefs.tab) { Widget.MUSIC })
    var musicApp by mutableStateOf(prefs.musicApp)
    var favs by mutableStateOf(prefs.favApps)
    var voice by mutableStateOf(prefs.voice)
    var avoidHighways by mutableStateOf(prefs.avoidHighways)
    var avoidTolls by mutableStateOf(prefs.avoidTolls)

    fun reload() {
        musicApp = prefs.musicApp; favs = prefs.favApps
        voice = prefs.voice; avoidHighways = prefs.avoidHighways; avoidTolls = prefs.avoidTolls
    }

    fun show(w: Widget) { widget = w; if (w != Widget.SEARCH) prefs.tab = w.ordinal }
    fun setMusic(pkg: String) { musicApp = pkg; prefs.musicApp = pkg }
    fun setFavList(l: List<String>) { favs = l; prefs.favApps = l }
    fun changeVoice(b: Boolean) { voice = b; prefs.voice = b }
    fun changeAvoidHighways(b: Boolean) { avoidHighways = b; prefs.avoidHighways = b }
    fun changeAvoidTolls(b: Boolean) { avoidTolls = b; prefs.avoidTolls = b }
}

@Composable
fun DashScreen(state: DashState, prefs: Prefs, nav: Navigator, location: Location?) {
    val ctx = LocalContext.current
    val cfg = LocalConfiguration.current
    val landscape = cfg.screenWidthDp > cfg.screenHeightDp
    var pickMusic by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().wallpaper().padding(10.dp)) {
        val map: @Composable (Modifier) -> Unit = { m -> MapCard(nav, location, onSearch = { state.show(Widget.SEARCH) }, modifier = m) }
        val panel: @Composable (Modifier) -> Unit = { m -> Panel(state, prefs, nav, { pickMusic = true }, m) }
        if (landscape) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                map(Modifier.weight(2f).fillMaxHeight())
                panel(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                map(Modifier.weight(1.2f).fillMaxWidth())
                panel(Modifier.weight(1f).fillMaxWidth())
            }
        }
    }

    if (pickMusic) {
        AppPickerDialog("Ton appli musique", "Applis musique détectées", { Apps.musicApps(ctx) }, { pickMusic = false }) {
            state.setMusic(it); pickMusic = false
        }
    }
}

@Composable
fun BigButton(text: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(30.dp)).background(color).clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
    ) { Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
}

// ───────────────────────── Panneau de droite ─────────────────────────

@Composable
private fun Panel(state: DashState, prefs: Prefs, nav: Navigator, pickMusic: () -> Unit, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusRow()
        Box(Modifier.weight(1f).fillMaxWidth().glass()) {
            when (state.widget) {
                Widget.MUSIC -> MusicPanel(state.musicApp, pickMusic)
                Widget.SEARCH -> SearchPanel(nav, prefs) { state.show(Widget.MUSIC) }
                Widget.PHONE -> PhonePanel(prefs)
                Widget.APPS -> AppsPanel(state.favs, state::setFavList)
                Widget.SETTINGS -> SettingsPanel(state, pickMusic)
            }
        }
        Dock(state)
    }
}

@Composable
private fun StatusRow() {
    val ctx = LocalContext.current
    val (time, battery) = rememberClockAndBattery()
    Row(Modifier.fillMaxWidth().height(52.dp).glass(20.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(time, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(10.dp))
        if (battery >= 0) Text("$battery%", color = if (battery < 20) Color(0xFFFF453A) else TextDim, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        RoundButton(Icons.Filled.Mic, "Assistant vocal", size = 40.dp, background = MicRed) {
            try {
                ctx.startActivity(Intent(Intent.ACTION_VOICE_COMMAND).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) {
                Toast.makeText(ctx, "Aucun assistant vocal trouvé", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
private fun Dock(state: DashState) {
    val ctx = LocalContext.current
    val music = Apps.entry(ctx, state.musicApp)
    BoxWithConstraints(Modifier.fillMaxWidth().glass(22.dp).padding(horizontal = 6.dp, vertical = 8.dp)) {
        // 5 icônes : leur taille s'adapte à la largeur du panneau (max 52 dp)
        val itemSize = minOf(52.dp, (maxWidth - 24.dp) / 5)
        CompositionLocalProvider(LocalDockSize provides itemSize) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                DockItem(null, Icons.Filled.Search, "Itinéraire", listOf(Color(0xFF4A90FF), GoBlue), state.widget == Widget.SEARCH) { state.show(Widget.SEARCH) }
                DockItem(music?.icon, Icons.Filled.MusicNote, "Musique", listOf(Color(0xFFFF5A7A), Color(0xFFE0245E)), state.widget == Widget.MUSIC) { state.show(Widget.MUSIC) }
                DockItem(null, Icons.Filled.Phone, "Appels", listOf(Color(0xFF5FE07A), Color(0xFF2DB84C)), state.widget == Widget.PHONE) { state.show(Widget.PHONE) }
                DockItem(null, Icons.Filled.Apps, "Applis", listOf(Color(0xFF8E8E9A), Color(0xFF55555F)), state.widget == Widget.APPS) { state.show(Widget.APPS) }
                DockItem(null, Icons.Filled.Settings, "Réglages", listOf(Color(0xFF6B6B78), Color(0xFF3A3A44)), state.widget == Widget.SETTINGS) { state.show(Widget.SETTINGS) }
            }
        }
    }
}

private val LocalDockSize = staticCompositionLocalOf { 50.dp }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockItem(appIcon: ImageBitmap?, icon: ImageVector, label: String, colors: List<Color>, selected: Boolean, onClick: () -> Unit) {
    val size = LocalDockSize.current
    val shape = RoundedCornerShape(size * 0.28f)
    Box(
        Modifier.size(size).clip(shape)
            .then(if (appIcon == null) Modifier.background(Brush.verticalGradient(colors)) else Modifier)
            .then(if (selected) Modifier.border(2.5.dp, Color.White, shape) else Modifier)
            .combinedClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (appIcon != null) Image(appIcon, label, Modifier.fillMaxSize())
        else Icon(icon, label, tint = Color.White, modifier = Modifier.size(size * 0.55f))
    }
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
