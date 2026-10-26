package app.equalizer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.equalizer.R
import app.equalizer.core.Band
import app.equalizer.core.EngineResult
import app.equalizer.core.EqController
import app.equalizer.core.GAIN_MAX
import app.equalizer.core.GAIN_MIN
import app.equalizer.ui.theme.EqColors
import app.equalizer.ui.theme.EqType

/** Screen S02: five bands, each a label, a chevron up, a bar with its value, a chevron down. */
@Composable
fun EqScreen(controller: EqController, onChanged: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.equalizer_switch), style = EqType.lg, color = EqColors.text, modifier = Modifier.weight(1f))
            Switch(
                checked = controller.enabled,
                onCheckedChange = { controller.setEnabled(it); onChanged() },
                colors = SwitchDefaults.colors(checkedTrackColor = EqColors.accent, checkedThumbColor = EqColors.onAccent),
            )
        }
        (controller.engineStatus as? EngineResult.Unavailable)?.let {
            Text(
                stringResource(R.string.engine_unavailable, it.reason),
                style = EqType.sm,
                color = EqColors.danger,
                modifier = Modifier.semantics { contentDescription = it.reason },
            )
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Band.entries.forEach { band -> BandColumn(controller, band, onChanged, Modifier.weight(1f)) }
        }
        TextButton(onClick = { controller.reset(); onChanged() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(R.string.reset), style = EqType.sm, color = EqColors.accent)
        }
    }
}

@Composable
private fun BandColumn(controller: EqController, band: Band, onChanged: () -> Unit, modifier: Modifier) {
    val gain = controller.state.gain(band)
    val label = if (band.hz >= 1000) "${band.hz / 1000}kHz" else "${band.hz}Hz"
    val signed = if (gain > 0) "+$gain dB" else "$gain dB"
    Column(
        modifier.semantics { contentDescription = "${band.hz} hertz"; stateDescription = signed },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = EqType.sm, color = EqColors.text)
        ChevronButton(up = true, enabled = controller.state.canRaise(band), description = "Raise $label by 1 dB") {
            controller.raise(band); onChanged()
        }
        Row(Modifier.weight(1f)) {
            Column(Modifier.fillMaxHeight().padding(end = 4.dp), verticalArrangement = Arrangement.SpaceBetween) {
                listOf(GAIN_MAX, 0, GAIN_MIN).forEach { Text("$it", style = EqType.xs, color = EqColors.textMuted) }
            }
            Box(
                Modifier.fillMaxHeight().weight(1f).clip(RoundedCornerShape(4.dp)).background(EqColors.surface),
                contentAlignment = Alignment.Center,
            ) {
                Text("$gain", style = EqType.lg, color = EqColors.text, textAlign = TextAlign.Center)
            }
        }
        ChevronButton(up = false, enabled = controller.state.canLower(band), description = "Lower $label by 1 dB") {
            controller.lower(band); onChanged()
        }
    }
}

@Composable
private fun ChevronButton(up: Boolean, enabled: Boolean, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp)) {
        Icon(
            imageVector = if (up) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = description,
            tint = if (enabled) EqColors.textMuted else EqColors.border,
        )
    }
}
