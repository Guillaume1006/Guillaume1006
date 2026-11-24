package fr.cockpit.auto

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.Home
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import fr.cockpit.auto.data.CockpitState
import fr.cockpit.auto.media.MediaListenerService
import fr.cockpit.auto.media.Message
import fr.cockpit.auto.media.Messages
import fr.cockpit.auto.ui.CockpitApp
import fr.cockpit.auto.ui.CockpitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Dessine les écrans au format d'un téléphone 20:9 en paysage (données d'exemple). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w900dp-h405dp-land-xhdpi")
class ScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun screens() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        CockpitState.requestedScreen.value = null
        for ((pkg, label) in listOf("com.spotify.music" to "Spotify", "deezer.android.app" to "Deezer")) {
            val pm = shadowOf(app.packageManager)
            pm.installPackage(PackageInfo().apply {
                packageName = pkg
                applicationInfo = ApplicationInfo().apply { packageName = pkg; nonLocalizedLabel = label }
            })
            val main = ComponentName(pkg, "$pkg.Main")
            pm.addActivityIfNotPresent(main)
            pm.addIntentFilterForActivity(main, IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) })
            val svc = ComponentName(pkg, "$pkg.Media")
            pm.addServiceIfNotPresent(svc)
            pm.addIntentFilterForService(svc, IntentFilter("android.media.browse.MediaBrowserService"))
        }
        Settings.Secure.putString(
            app.contentResolver, "enabled_notification_listeners",
            ComponentName(app, MediaListenerService::class.java).flattenToString(),
        )
        Messages.clear()
        val now = System.currentTimeMillis()
        Messages.add(Message("1", "com.whatsapp", "WhatsApp", "Garage (exemple)", "Votre voiture est prête, on ferme à 18 h.", now - 3_600_000, null, null))
        Messages.add(Message("2", "com.google.android.apps.messaging", "Messages", "Marie (exemple)", "J'arrive dans 10 min, je te garde une place devant.", now, null, null))

        compose.setContent { CockpitTheme { CockpitApp() } }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screens/1-tableau-de-bord.png")

        compose.onNodeWithContentDescription("Musique").performClick()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screens/2-choix-musique.png")
        compose.onNodeWithText("Spotify").performClick()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screens/3-lecteur.png")

        compose.onNodeWithContentDescription("Messages").performClick()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screens/4-messages.png")

        compose.onNodeWithContentDescription("Tableau de bord").performClick()
        compose.onNodeWithContentDescription("Toutes les apps").performClick()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screens/5-apps.png")
        compose.onNodeWithText("Téléphone").performClick()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screens/6-telephone.png")

        compose.onNodeWithContentDescription("Tableau de bord").performClick()
        compose.onNodeWithContentDescription("Toutes les apps").performClick()
        compose.onNodeWithText("Réglages").performClick()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screens/7-reglages.png")
    }

    /** Disposition du mode Waze : barre et colonne de Cockpit, Waze au milieu (zone grisée ici). */
    @Test
    fun waze_mode_layout() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val nowPlaying = fr.cockpit.auto.media.NowPlaying(app)
        compose.setContent {
            CockpitTheme {
                androidx.compose.foundation.layout.Row(androidx.compose.ui.Modifier.fillMaxSize().background(fr.cockpit.auto.ui.K.bg)) {
                    fr.cockpit.auto.ui.Rail(
                        claudeIcon = null, selected = null,
                        bottomIcon = androidx.compose.material.icons.Icons.Rounded.Home, bottomLabel = "Retour",
                        onNavigation = {}, onMusic = {}, onMessages = {}, onClaude = {}, onBottom = {},
                    )
                    androidx.compose.foundation.layout.Box(
                        androidx.compose.ui.Modifier.weight(1f).fillMaxHeight().background(androidx.compose.ui.graphics.Color(0xFF2B3A44)),
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                    ) {
                        androidx.compose.material3.Text("Waze s'affiche ici", color = androidx.compose.ui.graphics.Color.White, fontSize = 22.sp)
                    }
                    val col = (900f - 78f - 24f - 12f) / 2.6f + 24f
                    fr.cockpit.auto.ui.DashboardSide(
                        nowPlaying, 0, {},
                        androidx.compose.ui.Modifier.width(col.dp).fillMaxHeight().background(fr.cockpit.auto.ui.K.bg).padding(12.dp),
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screens/8-mode-waze.png")
    }
}
