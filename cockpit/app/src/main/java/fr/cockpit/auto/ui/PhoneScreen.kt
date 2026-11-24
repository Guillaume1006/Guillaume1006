package fr.cockpit.auto.ui

import android.Manifest
import android.content.Context
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.cockpit.auto.apps.Launchers
import fr.cockpit.auto.data.granted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Contact(val name: String, val number: String)

/** Favoris en grosses tuiles : un tap appelle. */
@Composable
fun PhoneScreen(refresh: Int) {
    val ctx = LocalContext.current
    var asked by remember { mutableIntStateOf(0) }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { asked++ }
    val canRead = remember(refresh, asked) { ctx.granted(Manifest.permission.READ_CONTACTS) }
    var favorites by remember { mutableStateOf<List<Contact>?>(null) }
    LaunchedEffect(canRead) { favorites = if (canRead) ctx.favoriteContacts() else emptyList() }

    KCard(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KLabel("Favoris", Modifier.weight(1f))
                KButton("Clavier", filled = false) { Launchers.openDialer(ctx) }
            }
            VSpace(10)
            val list = favorites
            when {
                !canRead -> {
                    Text("Autorise l'accès aux contacts pour appeler tes favoris d'un seul tap.", color = K.text, fontSize = 15.sp)
                    VSpace(12)
                    KButton("Autoriser") {
                        permissions.launch(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE))
                    }
                }
                list == null -> Unit
                list.isEmpty() -> Text(
                    "Aucun favori. Ajoute une étoile à tes contacts préférés dans l'app Contacts.",
                    color = K.dim, fontSize = 15.sp,
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(list, key = { it.name + it.number }) { c ->
                        Column(
                            Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (ctx.granted(Manifest.permission.CALL_PHONE)) {
                                        Launchers.call(ctx, c.number)
                                    } else {
                                        Launchers.toast(ctx, "Autorise les appels, puis touche à nouveau ${c.name}")
                                        permissions.launch(arrayOf(Manifest.permission.CALL_PHONE))
                                    }
                                }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Avatar(c.name, 64)
                            Spacer(Modifier.padding(top = 6.dp))
                            Text(
                                c.name, color = K.text, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun Context.favoriteContacts(): List<Contact> = withContext(Dispatchers.IO) {
    runCatching {
        contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            ),
            "${ContactsContract.CommonDataKinds.Phone.STARRED} = 1",
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC",
        )?.use { c ->
            buildList {
                while (c.moveToNext()) {
                    val name = c.getString(0) ?: continue
                    val number = c.getString(1) ?: continue
                    add(Contact(name, number))
                }
            }.distinctBy { it.name }
        } ?: emptyList()
    }.getOrDefault(emptyList())
}
