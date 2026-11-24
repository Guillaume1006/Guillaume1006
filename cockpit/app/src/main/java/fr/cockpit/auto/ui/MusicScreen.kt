package fr.cockpit.auto.ui

import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Folder
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.cockpit.auto.apps.Launchers
import fr.cockpit.auto.data.Prefs
import fr.cockpit.auto.media.Library
import fr.cockpit.auto.media.LibraryItem
import fr.cockpit.auto.media.LibraryState
import fr.cockpit.auto.media.NowPlaying
import kotlinx.coroutines.delay

/** Lecteur intégré : pilote l'app de musique choisie sans quitter Cockpit. */
@Composable
fun MusicScreen(nowPlaying: NowPlaying, refresh: Int) {
    val ctx = LocalContext.current
    val chosen by Prefs.musicPackage.collectAsState()
    var picking by remember { mutableStateOf(false) }
    LaunchedEffect(chosen) { nowPlaying.refresh() }

    val pkg = chosen
    if (pkg == null || picking) {
        KCard(Modifier.fillMaxSize()) {
            MusicAppPicker(
                refresh = refresh,
                title = "Choisis ton app de musique",
                modifier = Modifier.padding(14.dp),
                onPicked = { picking = false },
            )
        }
        return
    }

    val label = remember(pkg) { Launchers.label(ctx, pkg) }
    val library = remember(pkg) { Library(ctx.applicationContext, pkg) }
    DisposableEffect(library) {
        library.connect()
        onDispose { library.disconnect() }
    }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KCard(Modifier.weight(1.35f).fillMaxHeight()) {
            PlayerPanel(nowPlaying, pkg, label, onChangeApp = { picking = true })
        }
        KCard(Modifier.weight(1f).fillMaxHeight()) {
            LibraryPanel(library, label)
        }
    }
}

@Composable
private fun PlayerPanel(nowPlaying: NowPlaying, pkg: String, label: String, onChangeApp: () -> Unit) {
    val ctx = LocalContext.current
    val access by nowPlaying.hasAccess.collectAsState()
    val track by nowPlaying.track.collectAsState()
    val t = track?.takeIf { it.packageName == pkg }

    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(t?.playing, t?.updatedAt) {
        while (t?.playing == true) {
            tick = SystemClock.elapsedRealtime()
            delay(500)
        }
    }
    val position = remember(tick, t) { t?.currentPositionMs() ?: 0L }
    val duration = t?.durationMs ?: 0L

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(140.dp).clip(RoundedCornerShape(16.dp)).background(Tile.art)) {
                t?.art?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            }
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                KLabel(label)
                VSpace(4)
                Text(
                    t?.title ?: "Rien en lecture",
                    color = K.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    when {
                        !access -> "Autorise l'accès aux notifications pour voir le titre en cours"
                        t == null -> "Touche lecture pour reprendre sur $label"
                        else -> t.artist
                    },
                    color = K.dim, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                if (!access) {
                    VSpace(8)
                    KButton("Autoriser") { Launchers.openNotificationAccess(ctx) }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(K.line)) {
            val p = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
            Box(Modifier.fillMaxWidth(p).fillMaxHeight().background(K.text))
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatMs(position), color = K.dim, fontSize = 12.sp)
            Text(if (duration > 0) formatMs(duration) else "", color = K.dim, fontSize = 12.sp)
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundControl(Icons.Rounded.SkipPrevious, "Précédent", 60) { nowPlaying.previous() }
            Box(
                Modifier.size(76.dp).clip(CircleShape).background(K.text).clickable {
                    if (t != null) nowPlaying.playPause() else nowPlaying.playChosen()
                },
                contentAlignment = Alignment.Center,
            ) {
                val playing = t?.playing == true
                Icon(
                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (playing) "Pause" else "Lecture",
                    tint = K.bg, modifier = Modifier.size(42.dp),
                )
            }
            RoundControl(Icons.Rounded.SkipNext, "Suivant", 60) { nowPlaying.next() }
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KButton("Changer d'app", filled = false, onClick = onChangeApp)
            KButton("Ouvrir $label", filled = false) { Launchers.openMusicApp(ctx) }
        }
    }
}

@Composable
private fun LibraryPanel(library: Library, label: String) {
    val ctx = LocalContext.current
    val state by library.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        when (val s = state) {
            LibraryState.Connecting -> {
                KLabel("Bibliothèque")
                VSpace(10)
                Text("Connexion à $label…", color = K.dim, fontSize = 15.sp)
            }
            LibraryState.Unavailable -> {
                KLabel("Bibliothèque")
                VSpace(10)
                Text(
                    "$label ne partage pas ses playlists avec les autres apps. " +
                        "Les commandes de lecture marchent quand même.",
                    color = K.text, fontSize = 15.sp, lineHeight = 20.sp,
                )
                VSpace(12)
                KButton("Choisir dans $label", filled = false) { Launchers.openMusicApp(ctx) }
            }
            is LibraryState.Items -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (s.canGoBack) {
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).background(K.panel2).clickable { library.back() },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Retour", tint = K.text, modifier = Modifier.size(20.dp)) }
                        Spacer(Modifier.size(10.dp))
                    }
                    KLabel(s.title)
                }
                VSpace(8)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(s.items, key = { it.id }) { item -> LibraryRow(item) { library.open(item) } }
                }
            }
        }
    }
}

@Composable
private fun LibraryRow(item: LibraryItem, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(K.panel2), contentAlignment = Alignment.Center) {
            Icon(if (item.browsable) Icons.Rounded.Folder else Icons.Rounded.PlayArrow, null, tint = K.text, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(item.title, color = K.text, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (item.subtitle.isNotEmpty()) Text(item.subtitle, color = K.dim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RoundControl(icon: ImageVector, label: String, size: Int, onClick: () -> Unit) {
    Box(Modifier.size(size.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = K.text, modifier = Modifier.size((size * 0.62f).dp))
    }
}

private fun formatMs(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

/** Liste des apps de musique installées, pour choisir celle que pilote Cockpit. */
@Composable
fun MusicAppPicker(refresh: Int, title: String, modifier: Modifier = Modifier, onPicked: () -> Unit = {}) {
    val ctx = LocalContext.current
    val apps = remember(refresh) { Launchers.musicApps(ctx) }
    val chosen by Prefs.musicPackage.collectAsState()
    Column(modifier) {
        KLabel(title)
        VSpace(10)
        if (apps.isEmpty()) {
            Text("Aucune app de musique trouvée sur ce téléphone.", color = K.text, fontSize = 15.sp)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(apps, key = { it.packageName }) { app ->
                MusicAppRow(app, selected = app.packageName == chosen) {
                    Prefs.setMusicPackage(app.packageName)
                    onPicked()
                }
            }
        }
    }
}
