package fr.cockpit.auto

import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.drawable.Icon
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.service.notification.StatusBarNotification
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
import fr.cockpit.auto.data.ForegroundApp
import fr.cockpit.auto.data.ForegroundDetector
import fr.cockpit.auto.data.Prefs
import fr.cockpit.auto.data.RailSide
import fr.cockpit.auto.media.MediaListenerService
import fr.cockpit.auto.media.Message
import fr.cockpit.auto.media.Messages
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
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowSettings
import org.robolectric.shadows.ShadowWindowManagerImpl
import java.time.Duration

/**
 * Parcours complet de l'app sur un Android simulé : chaque bouton est pressé
 * et on vérifie ce qui s'affiche ou s'ouvre réellement.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w900dp-h405dp-land-xhdpi")
class FunctionalTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private var scenario: ActivityScenario<MainActivity>? = null
    private lateinit var realDetector: ForegroundDetector

    private class FakeDetector(var access: Boolean, var foreground: String?) : ForegroundDetector {
        override fun hasAccess(ctx: Context) = access
        override fun lastResumed(ctx: Context, sinceMs: Long) = foreground
    }

    @Before
    fun reset() {
        CockpitState.activityVisible.value = false
        CockpitState.requestedScreen.value = null
        CockpitState.carApps.clear()
        CockpitState.lastLaunchAt = -100_000L
        Messages.clear()
        ShadowSettings.setCanDrawOverlays(false)
        realDetector = ForegroundApp.detector
        Prefs.init(app)
        Prefs.setWazePanel(true)
        Prefs.setWazeWindow(true)
        Prefs.setRailSide(RailSide.Left)
        CockpitState.mapBounds = null
        // Accès aux notifications retiré (chaîne vide, comme sur un téléphone où il est désactivé)
        Settings.Secure.putString(app.contentResolver, "enabled_notification_listeners", "")
    }

    @After
    fun close() {
        scenario?.close()
        ForegroundApp.detector = realDetector
    }

    /* ---------- Waze et Claude : ouverts sans animation, barre gardée ---------- */

    @Test
    fun waze_opens_without_animation_and_keeps_the_rail() {
        installApp(WAZE, "Waze")
        installApp(MAPS, "Maps")
        launch()
        railButton("Waze").performClick()
        val intent = nextStarted()
        assertEquals(WAZE, intent?.component?.packageName)
        assertTrue("sans animation", intent!!.flags and Intent.FLAG_ACTIVITY_NO_ANIMATION != 0)
        assertTrue("barre flottante prévue au-dessus de Waze", WAZE in CockpitState.carApps)
    }

    @Test
    fun waze_opens_in_a_window_the_size_of_the_map_card() {
        installApp(WAZE, "Waze")
        launch()
        val map = CockpitState.mapBounds
        assertNotNull("cadre de la carte mesuré", map)
        assertTrue("carte plus large que haute", map!!.width() > map.height())
        railButton("Waze").performClick()
        val started = shadowOf(app).nextStartedActivityForResult
        assertEquals(WAZE, started.intent.component?.packageName)
        @Suppress("DEPRECATION")
        val bounds = started.options?.getParcelable<android.graphics.Rect>("android:activity.launchBounds")
        assertEquals("Waze demandé à la taille exacte du cadre", map, bounds)
    }

    @Test
    fun waze_window_can_be_turned_off() {
        installApp(WAZE, "Waze")
        Prefs.setWazeWindow(false)
        launch()
        railButton("Waze").performClick()
        val started = shadowOf(app).nextStartedActivityForResult
        assertEquals(WAZE, started.intent.component?.packageName)
        @Suppress("DEPRECATION")
        assertNull(started.options?.getParcelable<android.graphics.Rect>("android:activity.launchBounds"))
    }

    @Test
    fun touching_the_map_opens_waze() {
        installApp(WAZE, "Waze")
        launch()
        compose.onNodeWithText("Waze").performClick()
        assertEquals(WAZE, startedPackage())
    }

    @Test
    fun claude_button_opens_claude() {
        installApp(CLAUDE, "Claude")
        launch()
        railButton("Claude").performClick()
        assertEquals(CLAUDE, startedPackage())
        assertTrue(CLAUDE in CockpitState.carApps)
    }

    /* ---------- Lecteur musique intégré ---------- */

    @Test
    fun music_player_opens_inside_cockpit() {
        installApp(SPOTIFY, "Spotify", media = true)
        installApp(DEEZER, "Deezer", media = true)
        launch()

        // Premier passage : choix de l'app, dans Cockpit (aucune autre app ne s'ouvre)
        railButton("Musique").performClick()
        compose.waitForIdle()
        assertNull(startedPackage())
        compose.onNodeWithText("CHOISIS TON APP DE MUSIQUE").assertExists()
        compose.onNodeWithText("Spotify").assertExists()
        compose.onNodeWithText("Deezer").performClick()
        assertEquals(DEEZER, Prefs.musicPackage.value)
        assertEquals(DEEZER, app.getSharedPreferences("cockpit", Context.MODE_PRIVATE).getString("music_package", null))

        // Le lecteur s'affiche dans Cockpit
        compose.onNodeWithText("Rien en lecture").assertExists()
        compose.onNodeWithText("DEEZER").assertExists()
        assertNull("toujours dans Cockpit", startedPackage())

        // Lecture : la commande part directement à Deezer, sans l'ouvrir
        compose.onAllNodesWithContentDescription("Lecture")[0].performClick()
        val play = shadowOf(app).broadcastIntents.lastOrNull { it.action == Intent.ACTION_MEDIA_BUTTON }
        assertNotNull(play)
        assertEquals(DEEZER, play!!.`package`)
        assertNull(startedPackage())

        // On peut quand même ouvrir Deezer pour choisir une playlist
        compose.onNodeWithText("Ouvrir Deezer").performClick()
        assertEquals(DEEZER, startedPackage())
    }

    /* ---------- Messages intégrés ---------- */

    @Test
    fun messages_open_inside_cockpit() {
        grantNotificationAccess()
        launch()
        Messages.add(sample("whatsapp:1", "Marie", "J'arrive dans 10 min"))
        railButton("Messages").performClick()
        compose.waitForIdle()
        assertNull(startedPackage())
        compose.onAllNodesWithText("Marie")[0].assertExists()
        assertTrue(compose.onAllNodesWithText("J'arrive dans 10 min").fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithText("Écouter").assertExists()
    }

    @Test
    fun messages_ask_for_notification_access() {
        launch()
        railButton("Messages").performClick()
        compose.onNodeWithText("Autoriser").performClick()
        assertEquals(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, nextStarted()?.action)
    }

    @Test
    fun a_message_notification_is_read_and_can_be_answered() {
        val pi = PendingIntent.getBroadcast(app, 0, Intent("test.REPLY"), PendingIntent.FLAG_MUTABLE)
        val n = Notification.Builder(app, "c")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Marie")
            .setContentText("Tu es où ?")
            .setCategory(Notification.CATEGORY_MESSAGE)
            .addAction(
                Notification.Action.Builder(Icon.createWithResource(app, android.R.drawable.ic_menu_send), "Répondre", pi)
                    .addRemoteInput(RemoteInput.Builder("reply").build())
                    .build()
            )
            .build()
        @Suppress("DEPRECATION")
        val sbn = StatusBarNotification("com.whatsapp", "com.whatsapp", 1, null, 0, 0, 0, n, Process.myUserHandle(), 1_000L)
        val m = Messages.parse(app, sbn)
        assertNotNull(m)
        assertEquals("Marie", m!!.sender)
        assertEquals("Tu es où ?", m.text)
        assertNotNull(m.reply)
        assertTrue(Messages.reply(app, m, "J'arrive"))
        assertTrue(shadowOf(app).broadcastIntents.any { it.action == "test.REPLY" })

        // Une notification qui n'est pas un message est ignorée
        val other = Notification.Builder(app, "c").setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Mise à jour").setContentText("Terminé").build()
        @Suppress("DEPRECATION")
        val sbn2 = StatusBarNotification("com.android.vending", "com.android.vending", 2, null, 0, 0, 0, other, Process.myUserHandle(), 1_000L)
        assertNull(Messages.parse(app, sbn2))
    }

    /* ---------- Téléphone intégré ---------- */

    @Test
    fun phone_opens_favorites_inside_cockpit() {
        launch()
        railButton("Toutes les apps").performClick()
        compose.onNodeWithText("Téléphone").performClick()
        compose.onNodeWithText("FAVORIS").assertExists()
        assertNull(startedPackage())
        compose.onNodeWithText("Clavier").performClick()
        assertEquals(Intent.ACTION_DIAL, nextStarted()?.action)
    }

    /* ---------- Navigation dans Cockpit ---------- */

    @Test
    fun grid_settings_and_back_home() {
        launch()
        railButton("Toutes les apps").performClick()
        compose.onNodeWithText("Réglages").performClick()
        compose.onNodeWithText("BARRE LATÉRALE").assertExists()
        railButton("Tableau de bord").performClick()
        compose.onNodeWithText("Waze").assertExists()
    }

    @Test
    fun floating_rail_buttons_bring_back_cockpit_screens() {
        launch()
        CockpitState.requestedScreen.value = "music"
        compose.waitForIdle()
        compose.onNodeWithText("CHOISIS TON APP DE MUSIQUE").assertExists()
        CockpitState.requestedScreen.value = "messages"
        compose.waitForIdle()
        compose.onNodeWithText("MESSAGES").assertExists()
    }

    @Test
    fun rail_is_flush_left_and_can_move_right() {
        launch()
        val left = railButton("Waze").fetchSemanticsNode().boundsInRoot.left
        assertTrue("barre collée à gauche (x=$left)", left < 60f)
        railButton("Toutes les apps").performClick()
        compose.onNodeWithText("Réglages").performClick()
        compose.onNodeWithText("À droite").performClick()
        assertEquals(RailSide.Right, Prefs.railSide.value)
        val width = compose.onRoot().fetchSemanticsNode().size.width
        val x = railButton("Waze").fetchSemanticsNode().boundsInRoot.left
        assertTrue("barre à droite (x=$x, largeur=$width)", x > width / 2)
    }

    @Test
    fun overlay_chip_asks_for_overlay_permission() {
        launch()
        compose.onNodeWithText("Garder la barre visible dans Waze : autoriser").performClick()
        assertEquals(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, nextStarted()?.action)
    }

    /* ---------- Barre flottante ---------- */

    @Test
    fun floating_rail_only_over_waze_and_claude_not_over_phone_menus() {
        ShadowSettings.setCanDrawOverlays(true)
        val detector = FakeDetector(access = true, foreground = WAZE)
        ForegroundApp.detector = detector
        CockpitState.carApps += WAZE
        val controller = Robolectric.buildService(RailOverlayService::class.java).create()
        val wm = controller.get().getSystemService(WindowManager::class.java)
        idle()

        // Dans Waze : barre visible, collée au bord, en paysage, écran allumé
        val view = window(wm, RailOverlayService.TAG_RAIL)!!
        val params = view.layoutParams as WindowManager.LayoutParams
        assertEquals(View.VISIBLE, view.visibility)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, params.screenOrientation)
        assertEquals(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES, params.layoutInDisplayCutoutMode)
        assertTrue(params.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
        assertEquals(Gravity.START, params.gravity and Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK)

        // Retour aux menus du téléphone : la barre disparaît
        detector.foreground = "com.nothing.launcher"
        idleFor(600)
        assertEquals(View.GONE, view.visibility)

        // Retour dans Waze : elle revient
        detector.foreground = WAZE
        idleFor(600)
        assertEquals(View.VISIBLE, view.visibility)

        // Retour dans Cockpit : elle se cache (la barre de Cockpit est au même endroit)
        CockpitState.activityVisible.value = true
        idle()
        assertEquals(View.GONE, view.visibility)

        // Ouverture d'une app depuis Cockpit : affichée tout de suite, avant même la nouvelle app
        CockpitState.markLaunched(CLAUDE)
        idle()
        assertEquals(View.VISIBLE, view.visibility)

        // Barre réglée à droite
        Prefs.setRailSide(RailSide.Right)
        idle()
        assertEquals(Gravity.END, (view.layoutParams as WindowManager.LayoutParams).gravity and Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK)

        controller.destroy()
        assertTrue(wmViews(wm).isEmpty())
    }

    @Test
    fun in_waze_the_music_agenda_weather_column_stays_on_the_right() {
        ShadowSettings.setCanDrawOverlays(true)
        val detector = FakeDetector(access = true, foreground = WAZE)
        ForegroundApp.detector = detector
        CockpitState.carApps += setOf(WAZE, CLAUDE)
        val controller = Robolectric.buildService(RailOverlayService::class.java).create()
        val wm = controller.get().getSystemService(WindowManager::class.java)
        idle()

        // Dans Waze : barre à gauche + colonne à droite, Waze visible seulement au milieu
        val rail = window(wm, RailOverlayService.TAG_RAIL)!!
        val panel = window(wm, RailOverlayService.TAG_PANEL)!!
        assertEquals(View.VISIBLE, rail.visibility)
        assertEquals(View.VISIBLE, panel.visibility)
        val params = panel.layoutParams as WindowManager.LayoutParams
        assertEquals(Gravity.END, params.gravity and Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK)
        assertEquals(controller.get().panelWidthPx(), params.width)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, params.screenOrientation)
        // Même largeur que la colonne du tableau de bord (900 dp de large dans ce test)
        val density = app.resources.displayMetrics.density
        val expectedDp = (900f - 78f - 24f - 12f) / 2.6f + 24f
        assertEquals(expectedDp, params.width / density, 1.5f)

        // Dans Claude : la barre seule, pas la colonne
        detector.foreground = CLAUDE
        idleFor(600)
        assertEquals(View.VISIBLE, rail.visibility)
        assertEquals(View.GONE, panel.visibility)

        // Option désactivée dans les réglages : barre seule dans Waze
        detector.foreground = WAZE
        Prefs.setWazePanel(false)
        idleFor(600)
        assertEquals(View.VISIBLE, rail.visibility)
        assertEquals(View.GONE, panel.visibility)
        Prefs.setWazePanel(true)
        idleFor(600)
        assertEquals(View.VISIBLE, panel.visibility)

        // Menus du téléphone : tout disparaît
        detector.foreground = "com.nothing.launcher"
        idleFor(600)
        assertEquals(View.GONE, rail.visibility)
        assertEquals(View.GONE, panel.visibility)

        // Barre à droite : la colonne passe à gauche
        detector.foreground = WAZE
        Prefs.setRailSide(RailSide.Right)
        idleFor(600)
        assertEquals(Gravity.START, (panel.layoutParams as WindowManager.LayoutParams).gravity and Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK)

        controller.destroy()
        assertTrue(wmViews(wm).isEmpty())
    }

    @Test
    fun floating_rail_needs_the_overlay_permission() {
        ShadowSettings.setCanDrawOverlays(false)
        val controller = Robolectric.buildService(RailOverlayService::class.java).create()
        CockpitState.markLaunched(WAZE)
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

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithText(text: String) =
        onAllNodes(androidx.compose.ui.test.hasText(text))

    private fun wmViews(wm: WindowManager): List<View> = Shadow.extract<ShadowWindowManagerImpl>(wm).views

    private fun window(wm: WindowManager, tag: String): View? = wmViews(wm).firstOrNull { it.tag == tag }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun idleFor(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun nextStarted(): Intent? = shadowOf(app).nextStartedActivity

    private fun startedPackage(): String? {
        val i = nextStarted() ?: return null
        return i.component?.packageName ?: i.`package`
    }

    private fun drainStarted() {
        while (nextStarted() != null) Unit
    }

    private fun grantNotificationAccess() {
        Settings.Secure.putString(
            app.contentResolver,
            "enabled_notification_listeners",
            ComponentName(app, MediaListenerService::class.java).flattenToString(),
        )
    }

    private fun sample(key: String, sender: String, text: String) =
        Message(key, "com.whatsapp", "WhatsApp", sender, text, System.currentTimeMillis(), null, null)

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
