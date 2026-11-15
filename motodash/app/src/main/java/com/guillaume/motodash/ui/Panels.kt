package com.guillaume.motodash.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guillaume.motodash.AppEntry
import com.guillaume.motodash.Apps
import com.guillaume.motodash.Contact
import com.guillaume.motodash.LocationTracker
import com.guillaume.motodash.MediaRepo
import com.guillaume.motodash.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

// ───────────────────────── Compteur ─────────────────────────

@Composable
fun SpeedPanel(tracker: LocationTracker) {
    val ride by tracker.state.collectAsStateWithLifecycle()
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val mins = ((now - ride.startMs) / 60_000).coerceAtLeast(0)

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("Vitesse max", "${ride.maxKmh.toInt()}", "km/h", Modifier.weight(1f).fillMaxHeight())
            Stat("Trajet", String.format(Locale.FRANCE, "%.1f", ride.tripKm), "km", Modifier.weight(1f).fillMaxHeight())
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("Durée", "%d:%02d".format(mins / 60, mins % 60), "h", Modifier.weight(1f).fillMaxHeight())
            Stat("Altitude", ride.altitude?.toInt()?.toString() ?: "–", "m", Modifier.weight(1f).fillMaxHeight())
        }
        Row(
            Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(14.dp)).background(CardHi)
                .clickable { tracker.resetTrip() }.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Refresh, null, tint = Color.White)
            Text("  Nouveau trajet", color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
private fun Stat(label: String, value: String, unit: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(CardHi).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, color = TextDim, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(" $unit", color = TextDim, fontSize = 14.sp, modifier = Modifier.padding(bottom = 5.dp))
        }
    }
}

// ───────────────────────── Musique ─────────────────────────

@Composable
fun MusicPanel() {
    val ctx = LocalContext.current
    var info by remember { mutableStateOf(MediaRepo.read(ctx)) }
    LaunchedEffect(Unit) { while (true) { info = MediaRepo.read(ctx); delay(500) } }
    val art = remember(info.art) { info.art?.asImageBitmap() }
    val progress = if (info.durationMs > 0) (info.positionMs.toFloat() / info.durationMs).coerceIn(0f, 1f) else 0f

    Box(Modifier.fillMaxSize()) {
        // Pochette floutée en fond, comme sur CarPlay (flou sur Android 12+)
        if (art != null) {
            Image(art, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(40.dp).alpha(0.45f))
        }
        BoxWithConstraints(Modifier.fillMaxSize().padding(14.dp)) {
            val tall = maxHeight > 340.dp
            val artSize = if (tall) minOf(maxWidth * 0.6f, maxHeight * 0.42f) else (maxHeight - 150.dp).coerceIn(56.dp, 120.dp)

            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (!info.access) {
                    Text(
                        "Touchez ici pour afficher le titre en cours",
                        color = Accent, fontSize = 13.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CardHi)
                            .clickable { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                            .padding(6.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                }
                val cover: @Composable () -> Unit = {
                    Box(
                        Modifier.size(artSize).clip(RoundedCornerShape(14.dp)).background(CardHi)
                            .clickable { info.pkg?.let { Apps.launch(ctx, it) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (art != null) Image(art, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else Icon(Icons.Filled.MusicNote, null, tint = TextDim, modifier = Modifier.size(artSize * 0.45f))
                    }
                }
                val title = info.title ?: "Aucune lecture"
                val artist = info.artist ?: info.pkg?.let { Apps.entry(ctx, it)?.label } ?: "Lance ta musique"
                if (tall) {
                    cover()
                    Spacer(Modifier.height(12.dp))
                    Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(artist, color = Lilac, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        cover()
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(artist, color = Lilac, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                // Barre de progression
                Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.25f))) {
                    Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(Color.White))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    RoundButton(Icons.Filled.FastRewind, "Précédent", size = 64.dp, background = Color.Transparent) { MediaRepo.previous(ctx) }
                    RoundButton(
                        if (info.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Lecture / pause",
                        size = 72.dp, background = Color.White, tint = Color.Black,
                    ) { MediaRepo.playPause(ctx) }
                    RoundButton(Icons.Filled.FastForward, "Suivant", size = 64.dp, background = Color.Transparent) { MediaRepo.next(ctx) }
                }
            }
        }
    }
}

// ───────────────────────── Appels ─────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhonePanel(prefs: Prefs) {
    val ctx = LocalContext.current
    var contacts by remember { mutableStateOf(prefs.contacts) }
    var toRemove by remember { mutableStateOf<Contact?>(null) }
    var pendingCall by remember { mutableStateOf<String?>(null) }

    fun call(number: String) {
        val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        val action = if (granted) Intent.ACTION_CALL else Intent.ACTION_DIAL
        ctx.startActivity(Intent(action, Uri.parse("tel:" + Uri.encode(number))))
    }

    val callPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendingCall?.let { call(it) }; pendingCall = null
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val uri = res.data?.data ?: return@rememberLauncherForActivityResult
        if (res.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        ctx.contentResolver.query(
            uri,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, null,
        )?.use { c ->
            if (c.moveToFirst()) {
                contacts = contacts + Contact(c.getString(0) ?: "?", c.getString(1) ?: return@use)
                prefs.contacts = contacts
            }
        }
    }

    LazyVerticalGrid(
        GridCells.Adaptive(130.dp), Modifier.fillMaxSize().padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(contacts) { c ->
            Tile(
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                    if (granted) call(c.number) else { pendingCall = c.number; callPerm.launch(Manifest.permission.CALL_PHONE) }
                },
                onLongClick = { toRemove = c },
            ) {
                Box(Modifier.size(54.dp).clip(CircleShape).background(Accent), contentAlignment = Alignment.Center) {
                    Text(c.name.take(1).uppercase(), color = Color.Black, fontSize = 26.sp, fontWeight = FontWeight.Black)
                }
                Text(c.name, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        item {
            Tile(onClick = {
                pick.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
            }) {
                Icon(Icons.Filled.Add, null, tint = Accent, modifier = Modifier.size(48.dp))
                Text("Ajouter", color = TextDim, fontSize = 15.sp)
            }
        }
        item {
            Tile(onClick = { ctx.startActivity(Intent(Intent.ACTION_DIAL)) }) {
                Icon(Icons.Filled.Dialpad, null, tint = Color.White, modifier = Modifier.size(48.dp))
                Text("Clavier", color = TextDim, fontSize = 15.sp)
            }
        }
    }

    toRemove?.let { c ->
        ConfirmRemove(c.name, onDismiss = { toRemove = null }) {
            contacts = contacts - c; prefs.contacts = contacts; toRemove = null
        }
    }
}

// ───────────────────────── Applis ─────────────────────────

@Composable
fun AppsPanel(favs: List<String>, setFavs: (List<String>) -> Unit) {
    val ctx = LocalContext.current
    var picking by remember { mutableStateOf(false) }
    var toRemove by remember { mutableStateOf<AppEntry?>(null) }
    val entries = remember(favs) { favs.mapNotNull { Apps.entry(ctx, it) } }

    LazyVerticalGrid(
        GridCells.Adaptive(104.dp), Modifier.fillMaxSize().padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(entries) { app ->
            Tile(onClick = { Apps.launch(ctx, app.pkg) }, onLongClick = { toRemove = app }) {
                Image(app.icon, null, Modifier.size(52.dp))
                Text(app.label, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        item {
            Tile(onClick = { picking = true }) {
                Icon(Icons.Filled.Add, null, tint = Accent, modifier = Modifier.size(44.dp))
                Text("Ajouter", color = TextDim, fontSize = 14.sp)
            }
        }
    }

    if (picking) {
        AppPickerDialog("Ajouter une appli au dock", onDismiss = { picking = false }) { pkg ->
            if (pkg !in favs) setFavs(favs + pkg)
            picking = false
        }
    }
    toRemove?.let { a ->
        ConfirmRemove(a.label, onDismiss = { toRemove = null }) {
            setFavs(favs - a.pkg); toRemove = null
        }
    }
}

// ───────────────────────── Communs ─────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Tile(onClick: () -> Unit, onLongClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(18.dp)).background(CardHi)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) { content() }
}

@Composable
private fun ConfirmRemove(name: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Retirer $name ?") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Retirer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
        containerColor = Card,
    )
}

@Composable
fun AppPickerDialog(title: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val ctx = LocalContext.current
    val apps by produceState<List<AppEntry>?>(null) { value = withContext(Dispatchers.IO) { Apps.all(ctx) } }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxSize(0.9f).clip(RoundedCornerShape(24.dp)).background(Card).padding(16.dp)) {
            Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            val list = apps
            if (list == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Accent) }
            } else {
                LazyVerticalGrid(
                    GridCells.Adaptive(96.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(list, key = { it.pkg }) { app ->
                        Column(
                            Modifier.clip(RoundedCornerShape(14.dp)).clickable { onPick(app.pkg) }.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Image(app.icon, null, Modifier.size(52.dp))
                            Text(app.label, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
