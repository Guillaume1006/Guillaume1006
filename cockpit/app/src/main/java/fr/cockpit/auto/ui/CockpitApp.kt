package fr.cockpit.auto.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import fr.cockpit.auto.apps.Launchers
import fr.cockpit.auto.data.CockpitState
import fr.cockpit.auto.data.Prefs
import fr.cockpit.auto.data.RailSide
import fr.cockpit.auto.media.NowPlaying

enum class Screen { Dashboard, Apps, Settings }

@Composable
fun CockpitApp() {
    val context = LocalContext.current
    var screen by rememberSaveable { mutableStateOf(Screen.Dashboard) }

    // Incrémenté à chaque retour dans l'app ou réponse de permission :
    // relit les autorisations (notifications, agenda, position, superposition) et les icônes.
    var refresh by remember { mutableIntStateOf(0) }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refresh++
    }
    LaunchedEffect(Unit) {
        val asked = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_CALENDAR,
        )
        if (Build.VERSION.SDK_INT >= 33) asked += Manifest.permission.POST_NOTIFICATIONS
        permissions.launch(asked.toTypedArray())
    }
    OnResume { refresh++ }

    // Écran demandé depuis la barre flottante
    val requested by CockpitState.requestedScreen.collectAsState()
    LaunchedEffect(requested) {
        val r = requested ?: return@LaunchedEffect
        screen = when (r) {
            "settings" -> Screen.Settings
            "apps" -> Screen.Apps
            else -> Screen.Dashboard
        }
        CockpitState.requestedScreen.value = null
    }

    val nowPlaying = remember { NowPlaying(context.applicationContext) }
    DisposableEffect(refresh) {
        nowPlaying.start()
        onDispose { nowPlaying.stop() }
    }
    val claudeIcon = remember(refresh) { Launchers.claudeIcon(context) }
    val side by Prefs.railSide.collectAsState()

    // Musique : l'app choisie, sinon on propose de la choisir.
    val openMusic = {
        if (!Launchers.openMusic(context)) {
            Launchers.toast(context, "Choisis ton app de musique")
            screen = Screen.Settings
        }
    }

    BackHandler(enabled = screen != Screen.Dashboard) { screen = Screen.Dashboard }

    val rail = @Composable {
        Rail(
            claudeIcon = claudeIcon,
            bottomIcon = if (screen == Screen.Dashboard) Icons.Rounded.Apps else Icons.Rounded.Home,
            bottomLabel = if (screen == Screen.Dashboard) "Toutes les apps" else "Tableau de bord",
            onNavigation = { Launchers.openNavigation(context) },
            onMusic = openMusic,
            onMessages = { Launchers.openMessages(context) },
            onClaude = { Launchers.openClaude(context) },
            onBottom = { screen = if (screen == Screen.Dashboard) Screen.Apps else Screen.Dashboard },
        )
    }

    Row(
        Modifier
            .fillMaxSize()
            .background(K.bg)
            .windowInsetsPadding(WindowInsets.displayCutout)
    ) {
        if (side == RailSide.Left) rail()
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(12.dp)
        ) {
            when (screen) {
                Screen.Dashboard -> Dashboard(nowPlaying = nowPlaying, refresh = refresh, onOpenMusic = openMusic)
                Screen.Apps -> AppGrid(
                    claudeIcon = claudeIcon,
                    onOpenMusic = openMusic,
                    onOpenSettings = { screen = Screen.Settings },
                )
                Screen.Settings -> SettingsScreen(refresh = refresh)
            }
        }
        if (side == RailSide.Right) rail()
    }
}

/* ---------- Composants communs ---------- */

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

/** Bouton plein à la couleur d'accent. */
@Composable
fun KButton(text: String, modifier: Modifier = Modifier, filled: Boolean = true, onClick: () -> Unit) {
    Text(
        text,
        color = if (filled) K.accentInk else K.text,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (filled) K.accent else K.panel2)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
fun VSpace(h: Int) = Spacer(Modifier.height(h.dp))
