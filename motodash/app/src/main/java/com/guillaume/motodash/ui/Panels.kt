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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.guillaume.motodash.AppEntry
import com.guillaume.motodash.Apps
import com.guillaume.motodash.Contact
import com.guillaume.motodash.MediaRepo
import com.guillaume.motodash.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// ───────────────────────── Musique ─────────────────────────

@Composable
fun MusicPanel(musicApp: String?, onChoose: () -> Unit) {
    val ctx = LocalContext.current
    if (musicApp == null) {
        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Filled.MusicNote, null, tint = Color.White, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(12.dp))
            BigButton("Choisir mon appli musique", Color(0xFFE0245E), onChoose)
        }
        return
    }
    val app = Apps.entry(ctx, musicApp)
    var info by remember(musicApp) { mutableStateOf(MediaRepo.read(ctx, musicApp)) }
    LaunchedEffect(musicApp) { while (true) { info = MediaRepo.read(ctx, musicApp); delay(500) } }
    val art = remember(info.art) { info.art?.asImageBitmap() }
    val progress = if (info.durationMs > 0) (info.positionMs.toFloat() / info.durationMs).coerceIn(0f, 1f) else 0f

    Box(Modifier.fillMaxSize()) {
        // Pochette floutée en fond, comme sur CarPlay (flou sur Android 12+)
        if (art != null) {
            Image(art, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(40.dp).alpha(0.45f))
        }
        BoxWithConstraints(Modifier.fillMaxSize().padding(14.dp)) {
            if (maxHeight < 190.dp) {
                val title = info.title ?: (app?.label ?: "Musique")
                val artist = info.artist ?: "Appuie sur lecture"
                val cover: @Composable (androidx.compose.ui.unit.Dp) -> Unit = { size ->
                    Box(
                        Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(CardHi)
                            .clickable { Apps.launch(ctx, musicApp) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (art != null) Image(art, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else if (app != null) Image(app.icon, null, Modifier.size(size * 0.6f))
                    }
                }
                val texts: @Composable (Modifier) -> Unit = { m ->
                    Column(m.clickable(onClick = onChoose)) {
                        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(artist, color = Lilac, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                val controls: @Composable () -> Unit = {
                    RoundButton(Icons.Filled.FastRewind, "Précédent", size = 48.dp, background = Color.Transparent) { MediaRepo.previous(ctx, musicApp) }
                    RoundButton(if (info.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Lecture / pause", size = 58.dp, background = Color.White, tint = Color.Black) {
                        MediaRepo.playPause(ctx, musicApp)
                    }
                    RoundButton(Icons.Filled.FastForward, "Suivant", size = 48.dp, background = Color.Transparent) { MediaRepo.next(ctx, musicApp) }
                }
                if (maxWidth >= 330.dp) {
                    // Large et bas : tout sur une ligne
                    val coverSize = minOf(maxHeight, 64.dp)
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        cover(coverSize)
                        texts(Modifier.weight(1f).padding(horizontal = 10.dp))
                        controls()
                    }
                } else {
                    // Étroit et bas : pochette + titre, puis les boutons
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            cover(52.dp)
                            texts(Modifier.weight(1f).padding(start = 10.dp))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) { controls() }
                    }
                }
                return@BoxWithConstraints
            }
            val tall = maxHeight > 360.dp && maxWidth > 220.dp
            val artSize = if (tall) minOf(maxWidth * 0.62f, maxHeight * 0.4f) else (maxHeight - 175.dp).coerceIn(48.dp, 120.dp)
            val big = maxWidth > 260.dp

            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                // Appli choisie (toucher pour en changer)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onChoose).padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    app?.let { Image(it.icon, null, Modifier.size(22.dp).clip(RoundedCornerShape(6.dp))) }
                    Text("  " + (app?.label ?: musicApp), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text("Changer", color = TextDim, fontSize = 12.sp)
                }
                if (!info.access) {
                    Text(
                        "Autoriser l'affichage du titre",
                        color = Accent, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 1,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(10.dp)).background(CardHi)
                            .clickable { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                            .padding(6.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                val cover: @Composable () -> Unit = {
                    Box(
                        Modifier.size(artSize).clip(RoundedCornerShape(14.dp)).background(CardHi)
                            .clickable { Apps.launch(ctx, musicApp) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (art != null) Image(art, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else if (app != null) Image(app.icon, null, Modifier.size(artSize * 0.55f))
                        else Icon(Icons.Filled.MusicNote, null, tint = TextDim, modifier = Modifier.size(artSize * 0.45f))
                    }
                }
                val title = info.title ?: "Rien en lecture"
                val artist = info.artist ?: "Appuie sur lecture"
                if (tall) {
                    cover()
                    Spacer(Modifier.height(12.dp))
                    Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(artist, color = Lilac, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        cover()
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(artist, color = Lilac, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.25f))) {
                    Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(Color.White))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    RoundButton(Icons.Filled.FastRewind, "Précédent", size = if (big) 64.dp else 54.dp, background = Color.Transparent) {
                        MediaRepo.previous(ctx, musicApp)
                    }
                    RoundButton(
                        if (info.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Lecture / pause",
                        size = if (big) 74.dp else 66.dp, background = Color.White, tint = Color.Black,
                    ) { MediaRepo.playPause(ctx, musicApp) }
                    RoundButton(Icons.Filled.FastForward, "Suivant", size = if (big) 64.dp else 54.dp, background = Color.Transparent) {
                        MediaRepo.next(ctx, musicApp)
                    }
                }
            }
        }
    }
}

// ───────────────────────── Réglages ─────────────────────────

@Composable
fun SettingsPanel(state: DashState, pickMusic: () -> Unit) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Réglages", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        AppChoiceRow("Appli musique", Apps.entry(ctx, state.musicApp), pickMusic)
        SwitchRow("Guidage vocal", state.voice, state::changeVoice)
        SwitchRow("Carte nord en haut", state.northUp, state::changeNorthUp)
        SwitchRow("Éviter les autoroutes", state.avoidHighways, state::changeAvoidHighways)
        SwitchRow("Éviter les péages", state.avoidTolls, state::changeAvoidTolls)
        Text(
            "Accès au titre en cours",
            color = Color.White, fontSize = 15.sp,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardHi)
                .clickable { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                .padding(14.dp),
        )
        Text(
            "Carte © OpenStreetMap, OpenFreeMap. Itinéraires : Valhalla (profil moto). Recherche : Photon / Nominatim.",
            color = TextDim, fontSize = 12.sp,
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardHi).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Accent, checkedThumbColor = Color.White),
        )
    }
}

@Composable
private fun AppChoiceRow(label: String, app: AppEntry?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardHi).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (app != null) Image(app.icon, null, Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(label, color = TextDim, fontSize = 12.sp)
            Text(app?.label ?: "Choisir…", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("Changer", color = Accent, fontSize = 14.sp)
    }
}

// ───────────────────────── Appels ─────────────────────────

private fun callNumber(ctx: android.content.Context, number: String) {
    val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
    val action = if (granted) Intent.ACTION_CALL else Intent.ACTION_DIAL
    ctx.startActivity(Intent(action, Uri.parse("tel:" + Uri.encode(number))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** Appels depuis l'écran MotoDash : peut demander la permission et choisir un contact. */
@Composable
fun PhonePanel(prefs: Prefs) {
    val ctx = LocalContext.current
    var contacts by remember { mutableStateOf(prefs.contacts) }
    var toRemove by remember { mutableStateOf<Contact?>(null) }
    var pendingCall by remember { mutableStateOf<String?>(null) }

    val callPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendingCall?.let { callNumber(ctx, it) }; pendingCall = null
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

    ContactsGrid(
        contacts,
        onCall = { c ->
            val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
            if (granted) callNumber(ctx, c.number) else { pendingCall = c.number; callPerm.launch(Manifest.permission.CALL_PHONE) }
        },
        onLongClick = { toRemove = it },
        onAdd = { pick.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)) },
    )

    toRemove?.let { c ->
        ConfirmRemove(c.name, onDismiss = { toRemove = null }) {
            contacts = contacts - c; prefs.contacts = contacts; toRemove = null
        }
    }
}

@Composable
private fun ContactsGrid(contacts: List<Contact>, onCall: (Contact) -> Unit, onLongClick: ((Contact) -> Unit)?, onAdd: () -> Unit) {
    val ctx = LocalContext.current
    LazyVerticalGrid(
        GridCells.Adaptive(120.dp), Modifier.fillMaxSize().padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(contacts) { c ->
            Tile(onClick = { onCall(c) }, onLongClick = onLongClick?.let { f -> { f(c) } }) {
                Box(Modifier.size(54.dp).clip(CircleShape).background(Accent), contentAlignment = Alignment.Center) {
                    Text(c.name.take(1).uppercase(), color = Color.Black, fontSize = 26.sp, fontWeight = FontWeight.Black)
                }
                Text(c.name, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        item {
            Tile(onClick = onAdd) {
                Icon(Icons.Filled.Add, null, tint = Accent, modifier = Modifier.size(48.dp))
                Text("Ajouter", color = TextDim, fontSize = 15.sp)
            }
        }
        item {
            Tile(onClick = { ctx.startActivity(Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) {
                Icon(Icons.Filled.Dialpad, null, tint = Color.White, modifier = Modifier.size(48.dp))
                Text("Clavier", color = TextDim, fontSize = 15.sp)
            }
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
        AppPickerDialog("Ajouter une appli", onDismiss = { picking = false }) { pkg ->
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
fun AppPickerDialog(
    title: String,
    suggestedTitle: String? = null,
    suggested: (() -> List<AppEntry>)? = null,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    val ctx = LocalContext.current
    val data by produceState<Pair<List<AppEntry>, List<AppEntry>>?>(null) {
        value = withContext(Dispatchers.IO) {
            val sug = suggested?.invoke().orEmpty()
            val keys = sug.map { it.pkg }.toSet()
            sug to Apps.all(ctx).filter { it.pkg !in keys }
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxSize(0.92f).clip(RoundedCornerShape(24.dp)).background(Card).padding(16.dp)) {
            Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            val d = data
            if (d == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Accent) }
            } else {
                LazyVerticalGrid(
                    GridCells.Adaptive(92.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (d.first.isNotEmpty()) {
                        if (suggestedTitle != null) item(span = { GridItemSpan(maxLineSpan) }) { Header(suggestedTitle) }
                        items(d.first, key = { "s" + it.pkg }) { AppCell(it, onPick) }
                        item(span = { GridItemSpan(maxLineSpan) }) { Header("Toutes les applis") }
                    }
                    items(d.second, key = { it.pkg }) { AppCell(it, onPick) }
                }
            }
        }
    }
}

@Composable
private fun Header(text: String) {
    Text(text, color = Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun AppCell(app: AppEntry, onPick: (String) -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(14.dp)).clickable { onPick(app.pkg) }.padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(app.icon, null, Modifier.size(52.dp))
        Text(app.label, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
