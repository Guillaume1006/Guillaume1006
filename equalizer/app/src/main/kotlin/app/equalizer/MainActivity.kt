package app.equalizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.equalizer.core.EqController
import app.equalizer.core.InMemoryEqRepository
import app.equalizer.ui.EqScreen
import app.equalizer.ui.ProfileScreen
import app.equalizer.ui.theme.EqColors
import app.equalizer.ui.theme.EqType

class MainActivity : ComponentActivity() {
    // Fake data layer: in memory, same interface as the Room one that comes later.
    private val controller = EqController(InMemoryEqRepository(), DynamicsProcessingEngine())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var tab by remember { mutableIntStateOf(0) }
            // The controller is plain Kotlin, so a counter tells Compose when to redraw.
            var version by remember { mutableIntStateOf(0) }
            val changed = { version++ }
            Column(Modifier.fillMaxSize().background(EqColors.bg).safeDrawingPadding()) {
                TabRow(selectedTabIndex = tab, containerColor = EqColors.bg, contentColor = EqColors.accent) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }) {
                        Text(stringResource(R.string.tab_tune), style = EqType.sm, color = EqColors.text)
                    }
                    Tab(selected = tab == 1, onClick = { tab = 1 }) {
                        Text(stringResource(R.string.tab_profiles), style = EqType.sm, color = EqColors.text)
                    }
                }
                // Reading `version` here makes both screens redraw after a change.
                key(version) {
                    if (tab == 0) EqScreen(controller, changed) else ProfileScreen(controller, changed)
                }
            }
        }
    }
}
