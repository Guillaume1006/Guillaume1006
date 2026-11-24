package fr.cockpit.auto.ui

import android.app.Activity
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.cockpit.auto.apps.AppInfo
import fr.cockpit.auto.apps.Launchers
import fr.cockpit.auto.data.ForegroundApp
import fr.cockpit.auto.data.Prefs
import fr.cockpit.auto.data.RailSide
import fr.cockpit.auto.media.NowPlaying
import fr.cockpit.auto.overlay.RailOverlayService

@Composable
fun SettingsScreen(refresh: Int) {
    val ctx = LocalContext.current
    val side by Prefs.railSide.collectAsState()
    val overlayOk = remember(refresh) { Settings.canDrawOverlays(ctx) }
    val notificationsOk = remember(refresh) { NowPlaying.hasAccess(ctx) }
    val usageOk = remember(refresh) { ForegroundApp.detector.hasAccess(ctx) }
    val wazePanel by Prefs.wazePanel.collectAsState()
    val wazeWindow by Prefs.wazeWindow.collectAsState()

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KCard(Modifier.weight(1.1f).fillMaxHeight()) {
            Column(Modifier.fillMaxSize().padding(14.dp)) {
                MusicAppPicker(refresh = refresh, title = "App de musique", modifier = Modifier.weight(1f))
                VSpace(10)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KButton("Autorisations", filled = false) { Launchers.openAppSettings(ctx) }
                    KButton("Quitter le mode voiture", filled = false) {
                        RailOverlayService.stop(ctx)
                        (ctx as? Activity)?.finishAndRemoveTask()
                    }
                }
            }
        }

        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            KCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KLabel("Barre latérale", Modifier.weight(1f))
                        Choice("À gauche", side == RailSide.Left) { Prefs.setRailSide(RailSide.Left) }
                        Choice("À droite", side == RailSide.Right) { Prefs.setRailSide(RailSide.Right) }
                    }
                    StatusRow("Rester visible dans Waze et Claude", overlayOk) {
                        Launchers.openOverlayPermission(ctx)
                    }
                    StatusRow("Disparaître dans les menus du téléphone", usageOk) {
                        Launchers.openUsageAccess(ctx)
                    }
                    StatusRow("Lire la musique et les messages", notificationsOk) {
                        Launchers.openNotificationAccess(ctx)
                    }
                }
            }
            KCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KLabel("Waze")
                    ToggleRow("Waze en fenêtre, à la taille de la carte", wazeWindow) {
                        Prefs.setWazeWindow(!wazeWindow)
                    }
                    Text(
                        "« Fenêtres de forme libre » dans les options développeurs ›",
                        color = K.accent, fontSize = 12.sp, maxLines = 1,
                        modifier = Modifier.clickable { Launchers.openDeveloperOptions(ctx) },
                    )
                    ToggleRow("Garder musique, agenda et météo à droite", wazePanel) {
                        Prefs.setWazePanel(!wazePanel)
                    }
                }
            }
        }
    }
}

/** Ligne avec un bouton Oui / Non. */
@Composable
private fun ToggleRow(text: String, on: Boolean, onToggle: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = K.text, fontSize = 14.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.size(10.dp))
        Choice(if (on) "Oui" else "Non", on, compact = true, onClick = onToggle)
    }
}

@Composable
internal fun MusicAppRow(app: AppInfo, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) K.accent.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Tile.music), contentAlignment = Alignment.Center) {
            val icon = app.icon
            if (icon != null) Image(icon, null, Modifier.size(36.dp))
            else Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Text(
            app.label,
            color = K.text,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )
        if (selected) Icon(Icons.Rounded.CheckCircle, "Choisie", tint = K.accent)
        else Box(Modifier.size(22.dp).border(2.dp, K.line, CircleShape))
    }
}

@Composable
private fun Choice(label: String, selected: Boolean, compact: Boolean = false, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) K.accentInk else K.text,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) K.accent else K.panel2)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = if (compact) 6.dp else 9.dp),
    )
}

/** Ligne d'état : texte, puis « Activé » ou un bouton pour régler. [ok] null = simple lien. */
@Composable
private fun StatusRow(text: String, ok: Boolean?, onFix: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = K.text, fontSize = 14.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.size(10.dp))
        when (ok) {
            true -> Text("Activé", color = K.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            false -> KButton("Autoriser", onClick = onFix)
            null -> KButton("Ouvrir", filled = false, onClick = onFix)
        }
    }
}
