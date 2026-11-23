package com.guillaume.motodash.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guillaume.motodash.Prefs
import com.guillaume.motodash.nav.LatLon
import com.guillaume.motodash.nav.Navigator
import com.guillaume.motodash.nav.Place
import com.guillaume.motodash.nav.Search
import com.guillaume.motodash.nav.distance
import com.guillaume.motodash.nav.formatDistance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Choix de la destination : saisie ou dictée, favoris et destinations récentes. */
@Composable
fun SearchPanel(nav: Navigator, prefs: Prefs, onStarted: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var favorites by remember { mutableStateOf(prefs.favorites) }
    val recents = remember { prefs.recents }
    val focus = remember { FocusRequester() }

    fun run(q: String = query) {
        if (q.isBlank()) return
        scope.launch {
            loading = true; error = null
            results = try {
                withContext(Dispatchers.IO) { Search.search(q, nav.position) }.also { if (it.isEmpty()) error = "Aucun résultat" }
            } catch (_: Exception) { error = "Recherche impossible (pas de connexion ?)"; emptyList() }
            loading = false
        }
    }
    fun go(p: Place) { nav.start(p); onStarted() }
    fun toggleFav(p: Place) {
        favorites = if (favorites.any { it.lat == p.lat && it.lon == p.lon }) favorites.filterNot { it.lat == p.lat && it.lon == p.lon }
        else (listOf(p) + favorites).take(8)
        prefs.favorites = favorites
    }

    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val text = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: return@rememberLauncherForActivityResult
        query = text; run(text)
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        // Champ de recherche + micro
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(28.dp)).background(CardHi)
                    .clickable { focus.requestFocus() }.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, null, tint = TextDim)
                Box(Modifier.weight(1f).padding(start = 8.dp)) {
                    if (query.isEmpty()) Text("Où va-t-on ?", color = TextDim, fontSize = 17.sp, maxLines = 1)
                    BasicTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                        cursorBrush = SolidColor(Accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { run() }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                }
            }
            Spacer(Modifier.size(8.dp))
            RoundButton(Icons.Filled.Mic, "Dicter la destination", size = 52.dp, background = MicRed) {
                try {
                    speech.launch(
                        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Où va-t-on ?")
                    )
                } catch (_: Exception) {
                    Toast.makeText(ctx, "Dictée vocale indisponible sur ce téléphone", Toast.LENGTH_SHORT).show()
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        if (loading) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Accent) }
        }
        error?.let { Text(it, color = Accent, fontSize = 15.sp, modifier = Modifier.padding(8.dp)) }

        val here = nav.position
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (results.isNotEmpty()) {
                items(results) { p -> PlaceRow(p, Icons.Filled.Place, here, favorites.any { it.lat == p.lat && it.lon == p.lon }, { go(p) }, { toggleFav(p) }) }
            } else if (query.isBlank() || error != null) {
                if (favorites.isNotEmpty()) {
                    item { Section("Favoris") }
                    items(favorites) { p -> PlaceRow(p, Icons.Filled.Star, here, true, { go(p) }, { toggleFav(p) }) }
                }
                if (recents.isNotEmpty()) {
                    item { Section("Récents") }
                    items(recents) { p -> PlaceRow(p, Icons.Filled.History, here, favorites.any { it.lat == p.lat && it.lon == p.lon }, { go(p) }, { toggleFav(p) }) }
                }
                if (favorites.isEmpty() && recents.isEmpty()) item {
                    Text("Tape ou dicte une adresse, une ville ou un lieu.\nTouche ☆ pour l'ajouter aux favoris.", color = TextDim, fontSize = 15.sp, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, color = Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaceRow(p: Place, icon: ImageVector, here: LatLon?, fav: Boolean, onClick: () -> Unit, onFav: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardHi).combinedClickable(onClick = onClick).padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (icon == Icons.Filled.Star) Color(0xFFFFC107) else TextDim, modifier = Modifier.size(26.dp))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(p.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val dist = here?.let { formatDistance(distance(it, LatLon(p.lat, p.lon))) }
            val sub = listOfNotNull(dist, p.detail.ifBlank { null }).joinToString(" · ")
            if (sub.isNotBlank()) Text(sub, color = TextDim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        RoundButton(if (fav) Icons.Filled.Star else Icons.Filled.StarBorder, "Favori", size = 44.dp, background = Color.Transparent,
            tint = if (fav) Color(0xFFFFC107) else TextDim, onClick = onFav)
    }
}
