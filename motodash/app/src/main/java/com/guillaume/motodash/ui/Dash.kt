package com.guillaume.motodash.ui

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guillaume.motodash.LocationTracker
import com.guillaume.motodash.Prefs
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Tab(val label: String, val icon: ImageVector) {
    SPEED("Compteur", Icons.Filled.Speed),
    MUSIC("Musique", Icons.Filled.MusicNote),
    PHONE("Appels", Icons.Filled.Phone),
    APPS("Applis", Icons.Filled.Apps),
}

@Composable
fun DashScreen(tracker: LocationTracker, prefs: Prefs, locationGranted: Boolean) {
    val ride by tracker.state.collectAsStateWithLifecycle()
    val cfg = LocalConfiguration.current
    val landscape = cfg.screenWidthDp > cfg.screenHeightDp
    var mapFirst by remember { mutableStateOf(prefs.mapFirst) }

    val map: @Composable (Modifier) -> Unit = { m -> MapPanel(ride, prefs, locationGranted, m) }
    val side: @Composable (Modifier) -> Unit = { m ->
        SidePanel(tracker, prefs, ride.hasFix, onSwap = { mapFirst = !mapFirst; prefs.mapFirst = mapFirst }, modifier = m)
    }

    val gap = Arrangement.spacedBy(8.dp)
    val base = Modifier.fillMaxSize().background(Bg).padding(8.dp)
    if (landscape) {
        Row(base, horizontalArrangement = gap) {
            val m = Modifier.weight(1f).fillMaxHeight()
            if (mapFirst) { map(m); side(m) } else { side(m); map(m) }
        }
    } else {
        Column(base, verticalArrangement = gap) {
            val m = Modifier.weight(1f).fillMaxWidth()
            if (mapFirst) { map(m); side(m) } else { side(m); map(m) }
        }
    }
}

@Composable
fun SidePanel(tracker: LocationTracker, prefs: Prefs, hasFix: Boolean, onSwap: () -> Unit, modifier: Modifier) {
    var tab by remember { mutableIntStateOf(prefs.tab.coerceIn(0, Tab.entries.size - 1)) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusBar(hasFix, onSwap)
        Box(Modifier.weight(1f).fillMaxWidth().card()) {
            when (Tab.entries[tab]) {
                Tab.SPEED -> SpeedPanel(tracker)
                Tab.MUSIC -> MusicPanel()
                Tab.PHONE -> PhonePanel(prefs)
                Tab.APPS -> AppsPanel(prefs)
            }
        }
        Row(Modifier.fillMaxWidth().height(72.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tab.entries.forEachIndexed { i, t ->
                val sel = i == tab
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (sel) Accent else Card)
                        .clickable { tab = i; prefs.tab = i },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val c = if (sel) Color.Black else Color.White
                    Icon(t.icon, t.label, tint = c, modifier = Modifier.size(30.dp))
                    Text(t.label, color = c, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun StatusBar(hasFix: Boolean, onSwap: () -> Unit) {
    val ctx = LocalContext.current
    var time by remember { mutableStateOf("") }
    var battery by remember { mutableIntStateOf(-1) }
    LaunchedEffect(Unit) {
        val fmt = SimpleDateFormat("HH:mm", Locale.FRANCE)
        while (true) {
            time = fmt.format(Date())
            val b: Intent? = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = b?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = b?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            battery = if (level >= 0) level * 100 / scale else -1
            delay(5_000)
        }
    }
    Row(
        Modifier.fillMaxWidth().height(48.dp).card().padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(time, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Icon(
            if (hasFix) Icons.Filled.GpsFixed else Icons.Filled.GpsOff, "GPS",
            tint = if (hasFix) Color(0xFF4CD964) else Color(0xFFFF453A), modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        if (battery >= 0) {
            Icon(Icons.Filled.BatteryStd, "Batterie", tint = if (battery < 20) Color(0xFFFF453A) else Color.White, modifier = Modifier.size(22.dp))
            Text("$battery%", color = Color.White, fontSize = 18.sp)
        }
        Spacer(Modifier.width(10.dp))
        Icon(
            Icons.Filled.SwapHoriz, "Inverser carte / panneau", tint = TextDim,
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onSwap),
        )
    }
}
