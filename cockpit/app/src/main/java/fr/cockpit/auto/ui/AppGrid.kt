package fr.cockpit.auto.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.cockpit.auto.apps.Launchers

private class AppEntry(
    val label: String,
    val brush: Brush,
    val icon: ImageVector,
    val image: ImageBitmap? = null,
    val soon: Boolean = false,
    val onClick: () -> Unit = {},
)

@Composable
fun AppGrid(claudeIcon: ImageBitmap?, playingPackage: String?) {
    val ctx = LocalContext.current
    val apps = listOf(
        AppEntry("Navigation", Tile.nav, Icons.Rounded.Navigation) { Launchers.openNavigation(ctx) },
        AppEntry("Musique", Tile.music, Icons.Rounded.MusicNote) { Launchers.openMusic(ctx, playingPackage) },
        AppEntry("Téléphone", Tile.phone, Icons.Rounded.Phone) { Launchers.openDialer(ctx) },
        AppEntry("Messages", Tile.msg, Icons.AutoMirrored.Rounded.Chat) { Launchers.openMessages(ctx) },
        AppEntry("Claude", Tile.claude, Icons.Rounded.AutoAwesome, image = claudeIcon) { Launchers.openClaude(ctx) },
        AppEntry("Commandes", Tile.voice, Icons.Rounded.Mic, soon = true),
        AppEntry("Véhicule", Tile.car, Icons.Rounded.DirectionsCar, soon = true),
        AppEntry("Réglages", Tile.settings, Icons.Rounded.Settings) { Launchers.openAppSettings(ctx) },
    )

    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        apps.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { AppTile(it) }
            }
        }
    }
}

@Composable
private fun AppTile(app: AppEntry) {
    Column(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = !app.soon, onClick = app.onClick)
            .padding(8.dp)
            .alpha(if (app.soon) 0.4f else 1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(92.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(if (app.image != null) Brush.linearGradient(listOf(K.panel2, K.panel2)) else app.brush),
            contentAlignment = Alignment.Center,
        ) {
            if (app.image != null) Image(app.image, app.label, Modifier.size(92.dp))
            else Icon(app.icon, app.label, tint = Color.White, modifier = Modifier.size(42.dp))
        }
        Text(app.label, color = K.text, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp))
        if (app.soon) Text("Bientôt", color = K.dim, fontSize = 11.sp)
    }
}
