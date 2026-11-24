package fr.cockpit.auto.ui

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.cockpit.auto.apps.Launchers
import fr.cockpit.auto.data.CockpitState
import fr.cockpit.auto.media.Message
import fr.cockpit.auto.media.Messages
import fr.cockpit.auto.media.NowPlaying
import fr.cockpit.auto.media.Speaker
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Messages reçus (SMS, WhatsApp…) : lus à voix haute et réponse dictée, sans quitter Cockpit. */
@Composable
fun MessagesScreen(refresh: Int) {
    val ctx = LocalContext.current
    val access = remember(refresh) { NowPlaying.hasAccess(ctx) }
    val list by Messages.list.collectAsState()
    var selectedKey by remember { mutableStateOf<String?>(null) }
    val selected = list.firstOrNull { it.key == selectedKey } ?: list.firstOrNull()

    val speaker = remember { Speaker(ctx) }
    DisposableEffect(speaker) { onDispose { speaker.shutdown() } }

    var draft by remember { mutableStateOf<String?>(null) }
    val dictation = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        draft = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
    }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KCard(Modifier.weight(1f).fillMaxHeight()) {
            Column(Modifier.fillMaxSize().padding(14.dp)) {
                KLabel("Messages")
                VSpace(10)
                when {
                    !access -> {
                        Text(
                            "Autorise l'accès aux notifications pour recevoir tes SMS, WhatsApp, Messenger… ici.",
                            color = K.text, fontSize = 15.sp, lineHeight = 20.sp,
                        )
                        VSpace(12)
                        KButton("Autoriser") { Launchers.openNotificationAccess(ctx) }
                    }
                    list.isEmpty() -> Text(
                        "Aucun message pour l'instant. Les nouveaux SMS, WhatsApp, Messenger… s'afficheront ici.",
                        color = K.dim, fontSize = 15.sp, lineHeight = 20.sp,
                    )
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(list, key = { it.key }) { m ->
                            MessageRow(m, selected = m.key == selected?.key) {
                                selectedKey = m.key
                                draft = null
                            }
                        }
                    }
                }
            }
        }

        KCard(Modifier.weight(1.25f).fillMaxHeight()) {
            val m = selected
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                if (m == null) {
                    KLabel("Lecture")
                    VSpace(10)
                    Text("Touche un message pour l'écouter ou y répondre à la voix.", color = K.dim, fontSize = 15.sp)
                    return@Column
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(m.sender, 48)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(m.sender, color = K.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${m.appLabel} · ${formatClock(m.time)}", color = K.dim, fontSize = 13.sp)
                    }
                }
                VSpace(12)
                Text(
                    m.text,
                    color = K.text, fontSize = 20.sp, lineHeight = 27.sp,
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                )
                VSpace(10)
                val d = draft
                if (d != null) {
                    Text("Ta réponse : « $d »", color = K.accent, fontSize = 16.sp, lineHeight = 21.sp)
                    VSpace(10)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KButton("Envoyer") {
                            if (Messages.reply(ctx, m, d)) {
                                Launchers.toast(ctx, "Réponse envoyée à ${m.sender}")
                                speaker.say("Réponse envoyée")
                            } else {
                                Launchers.toast(ctx, "${m.appLabel} n'accepte pas de réponse directe")
                            }
                            draft = null
                        }
                        KButton("Annuler", filled = false) { draft = null }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KButton("Écouter") { speaker.say("${m.sender} : ${m.text}") }
                        if (m.reply != null) {
                            KButton("Répondre", filled = false) {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                                    .putExtra(RecognizerIntent.EXTRA_PROMPT, "Ta réponse à ${m.sender}")
                                runCatching { dictation.launch(intent) }
                                    .onFailure { Launchers.toast(ctx, "Dictée vocale indisponible") }
                            }
                        }
                        KButton("Ouvrir ${m.appLabel}", filled = false) { openConversation(ctx, m) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageRow(m: Message, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) K.accent.copy(alpha = 0.14f) else K.panel)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(m.sender, 38)
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(m.sender, color = K.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(formatClock(m.time), color = K.dim, fontSize = 12.sp)
            }
            Text(m.text, color = K.dim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun Avatar(name: String, size: Int) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(Tile.msg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase(),
            color = androidx.compose.ui.graphics.Color.White,
            fontSize = (size * 0.42f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun formatClock(ms: Long): String =
    Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))

/** Ouvre la conversation dans son app ; la barre flottante reste par-dessus. */
private fun openConversation(ctx: android.content.Context, m: Message) {
    val pi = m.open ?: return Launchers.toast(ctx, "Conversation introuvable")
    CockpitState.markLaunched(m.packageName)
    val options = ActivityOptions.makeCustomAnimation(ctx, 0, 0)
    if (Build.VERSION.SDK_INT >= 34) {
        @Suppress("DEPRECATION")
        options.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
    }
    try {
        pi.send(ctx, 0, null, null, null, null, options.toBundle())
    } catch (e: PendingIntent.CanceledException) {
        Launchers.toast(ctx, "Conversation introuvable")
    }
}
