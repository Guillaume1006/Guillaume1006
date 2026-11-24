package fr.cockpit.auto

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import fr.cockpit.auto.data.CockpitState
import fr.cockpit.auto.data.Prefs
import fr.cockpit.auto.data.RailSide
import fr.cockpit.auto.overlay.RailOverlayService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowSettings

/**
 * Parcours complet de l'app sur un Android simulé : chaque bouton est pressé
 * et on vérifie ce qui s'ouvre réellement.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w900dp-h405dp-land-xhdpi")
class FunctionalTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun reset() {
        CockpitState.activityVisible.value = false
        CockpitState.requestedScreen.value = null
        ShadowSettings.setCanDrawOverlays(false)
    }

    @After
    fun close() {
        scenario?.close()
    }

    /* ---------- Barre latérale ---------- */

    @Test
    fun waze_button_opens_waze() {
        installApp(WAZE, "Waze")
        installApp(MAPS, "Maps")
        launch()
        railButton("Waze").performClick()
        assertEquals(WAZE, startedPackage())
    }

    @Test
    fun map_card_button_opens_waze() {
        installApp(WAZE, "Waze")
        launch()
        compose.onNodeWithText("Ouvrir Waze").performClick()
        assertEquals(WAZE, startedPackage())
    }

    @Test
    fun claude_button_opens_claude() {
        installApp(CLAUDE, "Claude")
        launch()
        railButton("Claude").performClick()
        assertEquals(CLAUDE, startedPackage())
    }

    @Test
    fun music_asks_for_a_choice_then_opens_the_chosen_app() {
        installApp(SPOTIFY, "Spotify", media = true)
        installApp(DEEZER, "Deezer", media = true)
        launch()

        // Pas encore de choix : on arrive sur les réglages, rien d'autre ne s'ouvre
        railButton("Musique").performClick()
        compose.waitForIdle()
        assertNull(startedPackage())
        compose.onNodeWithText("APP DE MUSIQUE").assertExists()

        // Les deux lecteurs installés sont proposés ; on choisit Deezer
        compose.onNodeWithText("Spotify").assertExists()
        compose.onNodeWithText("Deezer").performClick()
        assertEquals(DEEZER, Prefs.musicPackage.value)
        assertEquals(
            DEEZER,
            app.getSharedPreferences("cockpit", Context.MODE_PRIVATE).getString("music_package", null),
        )

        // Le bouton Musique ouvre maintenant Deezer
        railButton("Musique").performClick()
        assertEquals(DEEZER, startedPackage())
    }

    @Test
    fun messages_button_opens_the_messaging_app() {
        launch()
        railButton("Messages").performClick()
        val intent = nextStarted()
        assertNotNull(intent)
        assertTrue(intent!!.selector?.hasCategory(Intent.CATEGORY_APP_MESSAGING) == true)
    }

    /* ---------- Grille d'apps ---------- */

    @Test
    fun grid_phone_opens_the_dialer() {
        launch()
        railButton("Toutes les apps").performClick()
        compose.onNodeWithText("Téléphone").performClick()
        assertEquals(Intent.ACTION_DIAL, nextStarted()?.action)
    }

    @Test
    fun grid_settings_and_home_navigation() {
        launch()
        railButton("Toutes les apps").performClick()
        compose.onNodeWithText("Réglages").performClick()
        compose.onNodeWithText("BARRE LATÉRALE").assertExists()
        railButton("Tableau de bord").performClick()
        compose.onNodeWithText("Ouvrir Waze").assertExists()
    }

    /* ---------- Autorisations ---------- */

    @Test
    fun music_card_asks_for_notification_access() {
        launch()
        compose.onNodeWithText("Autoriser").performClick()
        assertEquals(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, nextStarted()?.action)
    }

    @Test
    fun overlay_chip_asks_for_overlay_permission() {
        launch()
        compose.onNodeWithText("Garder la barre visible dans Waze : autoriser").performClick()
        assertEquals(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, nextStarted()?.action)
    }

    /* ---------- Côté de la barre ---------- */

    @Test
    fun rail_moves_to_the_right() {
        launch()
        railButton("Toutes les apps").performClick()
        compose.onNodeWithText("Réglages").performClick()
        compose.onNodeWithText("À droite").performClick()
        assertEquals(RailSide.Right, Prefs.railSide.value)
        val width = compose.onRoot().fetchSemanticsNode().size.width
        val x = railButton("Waze").fetchSemanticsNode().boundsInRoot.left
        assertTrue("barre à droite (x=$x, largeur=$width)", x > width / 2)
    }

    /* ---------- Barre flottante ---------- */

    @Test
    fun floating_rail_stays_landscape_and_hides_when_cockpit_is_visible() {
        ShadowSettings.setCanDrawOverlays(true)
        val controller = Robolectric.buildService(RailOverlayService::class.java).create()
        val wm = controller.get().getSystemService(WindowManager::class.java)
        idle()

        // Cockpit passe en arrière-plan (Waze ouvert) : la barre apparaît
        CockpitState.activityVisible.value = false
        idle()
        val views = wmViews(wm)
        assertEquals(1, views.size)
        val view = views[0]
        val params = view.layoutParams as WindowManager.LayoutParams
        assertEquals(View.VISIBLE, view.visibility)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, params.screenOrientation)
        assertTrue(params.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
        assertTrue(params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
        assertEquals(Gravity.START, params.gravity and Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK)

        // Barre réglée à droite
        Prefs.setRailSide(RailSide.Right)
        idle()
        assertEquals(Gravity.END, (view.layoutParams as WindowManager.LayoutParams).gravity and Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK)

        // Retour dans Cockpit : la barre flottante se cache
        CockpitState.activityVisible.value = true
        idle()
        assertEquals(View.GONE, view.visibility)

        controller.destroy()
        assertTrue(wmViews(wm).isEmpty())
    }

    @Test
    fun floating_rail_needs_the_overlay_permission() {
        ShadowSettings.setCanDrawOverlays(false)
        val controller = Robolectric.buildService(RailOverlayService::class.java).create()
        CockpitState.activityVisible.value = false
        idle()
        assertTrue(wmViews(controller.get().getSystemService(WindowManager::class.java)).isEmpty())
        controller.destroy()
    }

    @Test
    fun cockpit_starts_the_floating_rail_only_when_allowed() {
        ShadowSettings.setCanDrawOverlays(true)
        launch()
        val started = shadowOf(app).nextStartedService
        assertEquals(RailOverlayService::class.java.name, started?.component?.className)
    }

    /* ---------- Outils ---------- */

    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitForIdle()
        drainStarted() // demande de permissions au premier lancement
    }

    private fun railButton(label: String) = compose.onAllNodesWithContentDescription(label)[0]

    private fun wmViews(wm: WindowManager): List<View> =
        org.robolectric.shadow.api.Shadow.extract<org.robolectric.shadows.ShadowWindowManagerImpl>(wm).views

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun nextStarted(): Intent? = shadowOf(app).nextStartedActivity

    private fun startedPackage(): String? {
        val i = nextStarted() ?: return null
        return i.component?.packageName ?: i.`package`
    }

    private fun drainStarted() {
        while (nextStarted() != null) Unit
    }

    private fun installApp(pkg: String, label: String, media: Boolean = false) {
        val pm = shadowOf(app.packageManager)
        pm.installPackage(
            PackageInfo().apply {
                packageName = pkg
                applicationInfo = ApplicationInfo().apply {
                    packageName = pkg
                    nonLocalizedLabel = label
                }
            }
        )
        val main = ComponentName(pkg, "$pkg.Main")
        pm.addActivityIfNotPresent(main)
        pm.addIntentFilterForActivity(main, IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) })
        if (media) {
            val service = ComponentName(pkg, "$pkg.MediaService")
            pm.addServiceIfNotPresent(service)
            pm.addIntentFilterForService(service, IntentFilter("android.media.browse.MediaBrowserService"))
        }
    }

    private companion object {
        const val WAZE = "com.waze"
        const val MAPS = "com.google.android.apps.maps"
        const val CLAUDE = "com.anthropic.claude"
        const val SPOTIFY = "com.spotify.music"
        const val DEEZER = "deezer.android.app"
    }
}
