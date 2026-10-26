package app.equalizer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.equalizer.core.EqController
import app.equalizer.core.Presets
import app.equalizer.ui.theme.EqColors
import app.equalizer.ui.theme.EqType

/** Screen S01 (never seen, built from the recon's list of five profiles): pick one. */
@Composable
fun ProfileScreen(controller: EqController, onChanged: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp).selectableGroup()) {
        Presets.BUILTIN.forEach { preset ->
            val selected = controller.activePresetId == preset.id
            Row(
                Modifier.fillMaxWidth().selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    onClick = { controller.select(preset.id); onChanged() },
                ).padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selected,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = EqColors.accent, unselectedColor = EqColors.borderInput),
                )
                Text(preset.name, style = EqType.base, color = EqColors.text, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}
