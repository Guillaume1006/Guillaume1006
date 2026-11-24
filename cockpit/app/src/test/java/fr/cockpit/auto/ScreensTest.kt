package fr.cockpit.auto

import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import fr.cockpit.auto.ui.CockpitApp
import fr.cockpit.auto.ui.CockpitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot

/** Dessine les écrans au format d'un téléphone 20:9 en paysage. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w900dp-h405dp-land-xhdpi")
class ScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun screens() {
        compose.setContent { CockpitTheme { CockpitApp() } }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screens/1-tableau-de-bord.png")
        compose.onNodeWithContentDescription("Toutes les apps").performClick()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screens/2-apps.png")
    }
}
