package com.guillaume.motodash.ui

import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Widget { MUSIC, PHONE, APPS, SETTINGS }

/** État partagé de l'écran : applis choisies, widget affiché. */
class DashState(private val prefs: Prefs) {
    var widget by mutableStateOf(Widget.entries.getOrElse(prefs.tab) { Widget.MUSIC })
    var navApp by mutableStateOf(prefs.navApp)
    var musicApp by mutableStateOf(prefs.musicApp)
    var favs by mutableStateOf(prefs.favApps)
    var autoLaunch by mutableStateOf(prefs.autoLaunch)
    var overlayMode by mutableStateOf(prefs.overlayMode)

    /** Relit les réglages (modifiés depuis l'autre écran). */
    fun reload() {
        widget = Widget.entries.getOrElse(prefs.tab) { Widget.MUSIC }
        navApp = prefs.navApp; musicApp = prefs.musicApp; favs = prefs.favApps
        autoLaunch = prefs.autoLaunch; overlayMode = prefs.overlayMode
    }

    fun show(w: Widget) { widget = w; prefs.tab = w.ordinal }
    fun setNav(pkg: String) { navApp = pkg; prefs.navApp = pkg }
    fun setMusic(pkg: String) { musicApp = pkg; prefs.musicApp = pkg }
    fun setFavList(l: List<String>) { favs = l; prefs.favApps = l }
    fun setAuto(b: Boolean) { autoLaunch = b; prefs.autoLaunch = b }
    fun setOverlay(b: Boolean) { overlayMode = b; prefs.overlayMode = b }
}

@Composable
fun DashScreen(state: DashState, prefs: Prefs, split: Boolean) {
    val cfg = LocalConfiguration.current
    val landscape = cfg.screenWidthDp > cfg.screenHeightDp
    var picker by remember { mutableStateOf<Picker?>(null) }

    Box(Modifier.fillMaxSize().wallpaper().padding(10.dp)) {
        when {
            // MotoDash est dans sa moitié d'écran : on n'affiche que le panneau.
            split -> Panel(state, prefs, { picker = it }, Modifier.fillMaxSize())
            // Plein écran : la place du GPS (2/3) + le panneau (1/3).
            landscape -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GpsLauncher(state, { picker = it }, Modifier.weight(2f).fillMaxHeight())
                Panel(state, prefs, { picker = it }, Modifier.weight(1f).fillMaxHeight())
            }
            else -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GpsLauncher(state, { picker = it }, Modifier.weight(1f).fillMaxWidth())
                Panel(state, prefs, { picker = it }, Modifier.weight(1.3f).fillMaxWidth())
            }
        }
    }

    val ctx = LocalContext.current
    when (picker) {
        Picker.NAV -> AppPickerDialog("Ton appli GPS", "Applis GPS détectées", { Apps.navApps(ctx) }, { picker = null }) {
            state.setNav(it); picker = null; Drive.start(ctx, state)
        }
        Picker.MUSIC -> AppPickerDialog("Ton appli musique", "Applis musique détectées", { Apps.musicApps(ctx) }, { picker = null }) {
            state.setMusic(it); picker = null
        }
        null -> {}
    }
}

enum class Picker { NAV, MUSIC }

// ───────────────────────── Emplacement du GPS (plein écran) ─────────────────────────

@Composable
private fun GpsLauncher(state: DashState, pick: (Picker) -> Unit, modifier: Modifier) {
    val ctx = LocalContext.current
    val nav = Apps.entry(ctx, state.navApp)
    Column(
        modifier.glass().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (nav != null) {
            Image(nav.icon, null, Modifier.size(84.dp).clip(RoundedCornerShape(20.dp)))
            Spacer(Modifier.height(14.dp))
            BigButton(if (state.overlayMode) "Démarrer avec ${nav.label}" else "Ouvrir ${nav.label} à côté", GoBlue) { Drive.start(ctx, state) }
            Spacer(Modifier.height(10.dp))
            Text("Changer d'appli GPS", color = TextDim, fontSize = 14.sp, modifier = Modifier.clickable { pick(Picker.NAV) }.padding(6.dp))
        } else {
            Icon(Icons.Filled.Directions, null, tint = Color.White, modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(14.dp))
            BigButton("Choisir mon appli GPS", GoBlue) { pick(Picker.NAV) }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (state.overlayMode) "Le GPS s'ouvre en grand ici,\nMotoDash reste affiché sur le côté."
            else "Le GPS s'affiche ici, en écran partagé.\nFais glisser la barre du milieu pour lui donner plus de place.",
            color = TextDim, fontSize = 13.sp, textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun BigButton(text: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(30.dp)).background(color).clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
    ) { Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
}

// ───────────────────────── Panneau MotoDash ─────────────────────────

/**
 * Le panneau MotoDash. overlay = true quand il est affiché par-dessus le GPS
 * (pas de fenêtre de dialogue possible : les choix ouvrent l'écran MotoDash).
 */
@Composable
fun Panel(
    state: DashState, prefs: Prefs, pick: (Picker) -> Unit, modifier: Modifier,
    overlay: Boolean = false, onClose: (() -> Unit)? = null,
) {
    val ctx = LocalContext.current
    val openMain = { Drive.openMain(ctx, prefs, state.widget) }
    val widget: @Composable (Modifier) -> Unit = { m ->
        Box(m.glass()) {
            when (state.widget) {
                Widget.MUSIC -> MusicPanel(state.musicApp) { pick(Picker.MUSIC) }
                Widget.PHONE -> if (overlay) PhonePanelOverlay(prefs, openMain) else PhonePanel(prefs)
                Widget.APPS -> AppsPanel(state.favs, state::setFavList, if (overlay) openMain else null)
                Widget.SETTINGS -> SettingsPanel(state, pick)
            }
        }
    }
    BoxWithConstraints(modifier) {
        if (maxWidth > maxHeight * 1.25f) {
            // Panneau large et bas : le dock passe sur le côté
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusRow(onClose)
                    widget(Modifier.weight(1f).fillMaxWidth())
                }
                Dock(state, pick, vertical = true, overlay = overlay)
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusRow(onClose)
                widget(Modifier.weight(1f).fillMaxWidth())
                Dock(state, pick, vertical = false, overlay = overlay)
            }
        }
    }
}

@Composable
private fun StatusRow(onClose: (() -> Unit)?) {
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
        if (onClose != null) {
            Spacer(Modifier.width(8.dp))
            RoundButton(Icons.Filled.Close, "Fermer le panneau", size = 40.dp, background = Color.White.copy(alpha = 0.15f), onClick = onClose)
        }
    }
}

@Composable
private fun Dock(state: DashState, pick: (Picker) -> Unit, vertical: Boolean, overlay: Boolean) {
    val ctx = LocalContext.current
    val nav = Apps.entry(ctx, state.navApp)
    val music = Apps.entry(ctx, state.musicApp)
    val items: @Composable () -> Unit = {
        // GPS : rouvre l'appli GPS dans l'autre moitié
        DockItem(nav?.icon, Icons.Filled.Directions, "GPS", listOf(Color(0xFF4A90FF), GoBlue), false,
            onLongClick = { pick(Picker.NAV) }) {
            if (nav == null) pick(Picker.NAV) else if (state.overlayMode) Drive.openGps(ctx, nav.pkg) else Apps.launch(ctx, nav.pkg, adjacent = true)
        }
        DockItem(music?.icon, Icons.Filled.MusicNote, "Musique", listOf(Color(0xFFFF5A7A), Color(0xFFE0245E)),
            state.widget == Widget.MUSIC) { state.show(Widget.MUSIC) }
        DockItem(null, Icons.Filled.Phone, "Appels", listOf(Color(0xFF5FE07A), Color(0xFF2DB84C)),
            state.widget == Widget.PHONE) { state.show(Widget.PHONE) }
        DockItem(null, Icons.Filled.Apps, "Applis", listOf(Color(0xFF8E8E9A), Color(0xFF55555F)),
            state.widget == Widget.APPS) { state.show(Widget.APPS) }
        DockItem(null, Icons.Filled.Settings, "Réglages", listOf(Color(0xFF6B6B78), Color(0xFF3A3A44)),
            state.widget == Widget.SETTINGS) { state.show(Widget.SETTINGS) }
    }
    if (vertical) {
        BoxWithConstraints(Modifier.fillMaxHeight().glass(22.dp).padding(horizontal = 8.dp, vertical = 6.dp)) {
            // 5 icônes empilées : leur taille s'adapte à la hauteur (max 52 dp)
            val itemSize = minOf(52.dp, (maxHeight - 24.dp) / 5)
            CompositionLocalProvider(LocalDockSize provides itemSize) {
                Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) { items() }
            }
        }
    } else {
        BoxWithConstraints(Modifier.fillMaxWidth().glass(22.dp).padding(horizontal = 6.dp, vertical = 8.dp)) {
            // 5 icônes : leur taille s'adapte à la largeur du panneau (max 52 dp)
            val itemSize = minOf(52.dp, (maxWidth - 24.dp) / 5)
            CompositionLocalProvider(LocalDockSize provides itemSize) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) { items() }
            }
        }
    }
}

private val LocalDockSize = staticCompositionLocalOf { 50.dp }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockItem(
    appIcon: ImageBitmap?, icon: ImageVector, label: String, colors: List<Color>, selected: Boolean,
    onLongClick: (() -> Unit)? = null, onClick: () -> Unit,
) {
    val size = LocalDockSize.current
    val shape = RoundedCornerShape(size * 0.28f)
    Box(
        Modifier.size(size).clip(shape)
            .then(if (appIcon == null) Modifier.background(Brush.verticalGradient(colors)) else Modifier)
            .then(if (selected) Modifier.border(2.5.dp, Color.White, shape) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
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
