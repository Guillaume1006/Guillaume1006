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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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

    Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${ride.speedKmh.toInt()}", color = Color.White, fontSize = 110.sp, fontWeight = FontWeight.Black, lineHeight = 110.sp)
                Text("km/h", color = Accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("Max", "${ride.maxKmh.toInt()}", "km/h", Modifier.weight(1f))
            Stat("Trajet", String.format(Locale.FRANCE, "%.1f", ride.tripKm), "km", Modifier.weight(1f))
            Stat("Durée", "%d:%02d".format(mins / 60, mins % 60), "h", Modifier.weight(1f))
            Stat("Alt.", ride.altitude?.toInt()?.toString() ?: "–", "m", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.clip(RoundedCornerShape(14.dp)).background(CardHi).clickable { tracker.resetTrip() }
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Refresh, null, tint = Color.White)
            Text("  Remise à zéro", color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
private fun Stat(label: String, value: String, unit: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(CardHi).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = TextDim, fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(unit, color = TextDim, fontSize = 11.sp)
    }
}

// ───────────────────────── Musique ─────────────────────────

@Composable
fun MusicPanel() {
    val ctx = LocalContext.current
    var info by remember { mutableStateOf(MediaRepo.read(ctx)) }
    LaunchedEffect(Unit) { while (true) { info = MediaRepo.read(ctx); delay(1000) } }
    val art = remember(info.art) { info.art?.asImageBitmap() }

    Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (!info.access) {
            Text(
                "Touchez ici pour autoriser MotoDash à afficher le titre en cours",
                color = Accent, fontSize = 14.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CardHi)
                    .clickable { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                    .padding(10.dp),
            )
            Spacer(Modifier.height(8.dp))
        }
        Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true).clip(RoundedCornerShape(16.dp)).background(CardHi)
                    .clickable { info.pkg?.let { Apps.launch(ctx, it) } },
                contentAlignment = Alignment.Center,
            ) {
                if (art != null) Image(art, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Icon(Icons.Filled.MusicNote, null, tint = TextDim, modifier = Modifier.size(56.dp))
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(info.title ?: "Aucune lecture", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(info.artist ?: "", color = TextDim, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                info.pkg?.let { p -> Apps.entry(ctx, p)?.let { Text(it.label, color = Accent, fontSize = 13.sp) } }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            RoundButton(Icons.Filled.SkipPrevious, "Précédent", size = 72.dp, background = CardHi) { MediaRepo.previous(ctx) }
            RoundButton(if (info.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Lecture / pause", size = 92.dp, active = true) {
                MediaRepo.playPause(ctx)
            }
            RoundButton(Icons.Filled.SkipNext, "Suivant", size = 72.dp, background = CardHi) { MediaRepo.next(ctx) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            RoundButton(Icons.AutoMirrored.Filled.VolumeDown, "Volume -", background = CardHi) { MediaRepo.volume(ctx, false) }
            RoundButton(Icons.AutoMirrored.Filled.VolumeUp, "Volume +", background = CardHi) { MediaRepo.volume(ctx, true) }
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
fun AppsPanel(prefs: Prefs) {
    val ctx = LocalContext.current
    var favs by remember { mutableStateOf(prefs.favApps) }
    var picking by remember { mutableStateOf(false) }
    var toRemove by remember { mutableStateOf<AppEntry?>(null) }
    val entries = remember(favs) { favs.mapNotNull { Apps.entry(ctx, it) } }

    LazyVerticalGrid(
        GridCells.Adaptive(110.dp), Modifier.fillMaxSize().padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(entries) { app ->
            Tile(onClick = { Apps.launch(ctx, app.pkg) }, onLongClick = { toRemove = app }) {
                Image(app.icon, null, Modifier.size(56.dp))
                Text(app.label, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        item {
            Tile(onClick = { picking = true }) {
                Icon(Icons.Filled.Add, null, tint = Accent, modifier = Modifier.size(48.dp))
                Text("Ajouter", color = TextDim, fontSize = 14.sp)
            }
        }
    }

    if (picking) {
        AppPickerDialog("Ajouter une appli", onDismiss = { picking = false }) { pkg ->
            if (pkg !in favs) { favs = favs + pkg; prefs.favApps = favs }
            picking = false
        }
    }
    toRemove?.let { a ->
        ConfirmRemove(a.label, onDismiss = { toRemove = null }) {
            favs = favs - a.pkg; prefs.favApps = favs; toRemove = null
        }
    }
}

// ───────────────────────── Communs ─────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Tile(onClick: () -> Unit, onLongClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(18.dp)).background(CardHi)
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
