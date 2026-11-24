package fr.cockpit.auto.ui

import android.content.Context
import android.os.BatteryManager
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Largeur de la barre, dans l'app comme en barre flottante. */
val RAIL_WIDTH = 78.dp

/** Bouton de la barre correspondant à l'écran affiché (petit trait d'accent à côté). */
enum class RailItem { Navigation, Music, Messages }

/**
 * Barre latérale : heure, batterie, Waze, musique, messages, Claude, et un bouton du bas
 * (grille d'apps dans Cockpit, retour à Cockpit dans la barre flottante).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Rail(
    claudeIcon: ImageBitmap?,
    selected: RailItem?,
    bottomIcon: ImageVector,
    bottomLabel: String,
    onNavigation: () -> Unit,
    onMusic: () -> Unit,
    onMessages: () -> Unit,
    onClaude: () -> Unit,
    onBottom: () -> Unit,
    onBottomLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val now = rememberTicker(1_000)
    val time = remember(now / 60_000) { LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) }
    val battery = remember(now / 60_000) { batteryInfo(context) }

    Column(
        modifier
            .width(RAIL_WIDTH)
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
        RailButton(Tile.nav, Icons.Rounded.Navigation, "Waze", selected == RailItem.Navigation, onNavigation)
        RailButton(Tile.music, Icons.Rounded.MusicNote, "Musique", selected == RailItem.Music, onMusic)
        RailButton(Tile.msg, Icons.AutoMirrored.Rounded.Chat, "Messages", selected == RailItem.Messages, onMessages)
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
                .combinedClickable(onClick = onBottom, onLongClick = onBottomLongClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(bottomIcon, bottomLabel, tint = K.text)
        }
    }
}

@Composable
private fun RailButton(brush: Brush, icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.width(RAIL_WIDTH), contentAlignment = Alignment.Center) {
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 3.dp)
                    .size(width = 4.dp, height = 26.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(K.accent)
            )
        }
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
}

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

private fun batteryInfo(ctx: Context): Pair<Int, Boolean> {
    val bm = ctx.getSystemService(BatteryManager::class.java) ?: return -1 to false
    return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) to bm.isCharging
}
