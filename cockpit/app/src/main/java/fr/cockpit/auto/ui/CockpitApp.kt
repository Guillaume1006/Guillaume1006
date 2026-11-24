package fr.cockpit.auto.ui

import android.Manifest
import android.content.Context
import android.os.BatteryManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import fr.cockpit.auto.apps.Launchers
import fr.cockpit.auto.media.NowPlaying
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class Screen { Dashboard, Apps }

@Composable
fun CockpitApp() {
    val context = LocalContext.current
    var screen by rememberSaveable { mutableStateOf(Screen.Dashboard) }

    // Incrémenté à chaque retour dans l'app ou réponse de permission :
    // relit les autorisations (notifications, agenda, position) et l'icône de Claude.
    var refresh by remember { mutableIntStateOf(0) }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refresh++
    }
    LaunchedEffect(Unit) {
        permissions.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_CALENDAR,
            )
        )
    }
    OnResume { refresh++ }

    val nowPlaying = remember { NowPlaying(context.applicationContext) }
    DisposableEffect(refresh) {
        nowPlaying.start()
        onDispose { nowPlaying.stop() }
    }
    val track by nowPlaying.track.collectAsState()
    val claudeIcon = remember(refresh) { Launchers.claudeIcon(context) }

    BackHandler(enabled = screen != Screen.Dashboard) { screen = Screen.Dashboard }

    Row(
        Modifier
            .fillMaxSize()
            .background(K.bg)
            .windowInsetsPadding(WindowInsets.displayCutout)
    ) {
        Rail(
            screen = screen,
            claudeIcon = claudeIcon,
            onNavigation = { Launchers.openNavigation(context) },
            onMusic = { Launchers.openMusic(context, track?.packageName) },
            onMessages = { Launchers.openMessages(context) },
            onClaude = { Launchers.openClaude(context) },
            onHomeOrApps = {
                screen = if (screen == Screen.Dashboard) Screen.Apps else Screen.Dashboard
            },
        )
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(12.dp)
        ) {
            when (screen) {
                Screen.Dashboard -> Dashboard(nowPlaying = nowPlaying, refresh = refresh)
                Screen.Apps -> AppGrid(claudeIcon = claudeIcon, playingPackage = track?.packageName)
            }
        }
    }
}

/* ---------- Barre latérale ---------- */

@Composable
private fun Rail(
    screen: Screen,
    claudeIcon: ImageBitmap?,
    onNavigation: () -> Unit,
    onMusic: () -> Unit,
    onMessages: () -> Unit,
    onClaude: () -> Unit,
    onHomeOrApps: () -> Unit,
) {
    val context = LocalContext.current
    val now = rememberTicker(1_000)
    val time = remember(now / 60_000) { LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) }
    val battery = remember(now / 60_000) { batteryInfo(context) }

    Column(
        Modifier
            .width(78.dp)
            .fillMaxHeight()
            .background(K.rail)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(time, color = K.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        if (battery.first in 0..100) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.offset(y = (-8).dp)) {
                if (battery.second) Icon(Icons.Rounded.Bolt, null, tint = K.accent, modifier = Modifier.size(12.dp))
                Text("${battery.first} %", color = K.dim, fontSize = 11.sp, maxLines = 1)
            }
        }
        RailButton(Tile.nav, Icons.Rounded.Navigation, "Navigation", onNavigation)
        RailButton(Tile.music, Icons.Rounded.MusicNote, "Musique", onMusic)
        RailButton(Tile.msg, Icons.AutoMirrored.Rounded.Chat, "Messages", onMessages)
        Spacer(Modifier.weight(1f))
        // Claude toujours à un tap, sur tous les écrans
        Box(
            Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (claudeIcon == null) Tile.claude else Brush.linearGradient(listOf(K.panel2, K.panel2)))
                .clickable(onClickLabel = "Ouvrir Claude", onClick = onClaude),
            contentAlignment = Alignment.Center,
        ) {
            if (claudeIcon != null) Image(claudeIcon, "Claude", Modifier.size(50.dp))
            else Icon(Icons.Rounded.AutoAwesome, "Claude", tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Box(
            Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(K.panel2)
                .clickable(onClick = onHomeOrApps),
            contentAlignment = Alignment.Center,
        ) {
            val isHome = screen == Screen.Apps
            Icon(
                if (isHome) Icons.Rounded.Home else Icons.Rounded.Apps,
                if (isHome) "Tableau de bord" else "Toutes les apps",
                tint = K.text,
            )
        }
    }
}

@Composable
private fun RailButton(brush: Brush, icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(brush)
            .clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

/* ---------- Utilitaires ---------- */

/** Heure courante, rafraîchie à chaque période (alignée sur la seconde). */
@Composable
fun rememberTicker(periodMs: Long): Long {
    var t by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(periodMs) {
        while (true) {
            delay(periodMs - System.currentTimeMillis() % periodMs)
            t = System.currentTimeMillis()
        }
    }
    return t
}

/** Appelle [action] chaque fois que l'activité revient au premier plan. */
@Composable
fun OnResume(action: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) action()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}

private fun batteryInfo(ctx: Context): Pair<Int, Boolean> {
    val bm = ctx.getSystemService(BatteryManager::class.java) ?: return 0 to false
    return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) to bm.isCharging
}

/** Petit libellé en capitales des cartes. */
@Composable
fun KLabel(text: String, modifier: Modifier = Modifier, color: Color = K.dim) {
    Text(
        text.uppercase(),
        modifier = modifier,
        color = color,
        fontSize = 11.sp,
        letterSpacing = 1.3.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
    )
}

@Composable
fun KCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(K.panel)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) { content() }
}

@Composable
fun VSpace(h: Int) = Spacer(Modifier.height(h.dp))
