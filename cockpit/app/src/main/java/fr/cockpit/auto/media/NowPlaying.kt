package fr.cockpit.auto.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.KeyEvent
import fr.cockpit.auto.apps.Launchers
import fr.cockpit.auto.data.Prefs
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Une fois autorisé dans Réglages > Accès aux notifications, ce service ouvre l'accès
 * aux sessions média des autres apps et reçoit les messages (SMS, WhatsApp…) pour l'écran Messages.
 * (Nom conservé : le renommer ferait perdre l'autorisation déjà donnée.)
 */
class MediaListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        runCatching { activeNotifications }.getOrNull()?.sortedBy { it.postTime }?.forEach { onNotificationPosted(it) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Messages.parse(this, sbn)?.let { Messages.add(it) }
    }
}

data class Track(
    val title: String,
    val artist: String,
    val appLabel: String,
    val packageName: String,
    val art: Bitmap?,
    val playing: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
    val speed: Float,
) {
    /** Position estimée maintenant, sans attendre le prochain rapport de l'app. */
    fun currentPositionMs(): Long {
        if (!playing) return positionMs
        val elapsed = SystemClock.elapsedRealtime() - updatedAt
        val pos = positionMs + (elapsed * speed).toLong()
        return if (durationMs > 0) pos.coerceIn(0, durationMs) else pos
    }
}

/** Suit l'app audio active (Spotify, Deezer, YouTube Music, radio...) et la pilote. */
class NowPlaying(private val ctx: Context) {
    private val manager = ctx.getSystemService(MediaSessionManager::class.java)
    private val listener = ComponentName(ctx, MediaListenerService::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var controller: MediaController? = null

    private val _track = MutableStateFlow<Track?>(null)
    val track: StateFlow<Track?> = _track.asStateFlow()

    private val _hasAccess = MutableStateFlow(false)
    val hasAccess: StateFlow<Boolean> = _hasAccess.asStateFlow()

    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        pick(list.orEmpty())
    }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onSessionDestroyed() = refresh()
    }

    fun start() {
        stop()
        val ok = hasAccess(ctx)
        _hasAccess.value = ok
        if (!ok) {
            _track.value = null
            return
        }
        try {
            manager.addOnActiveSessionsChangedListener(sessionsChanged, listener, handler)
            pick(manager.getActiveSessions(listener))
        } catch (e: Exception) {
            _hasAccess.value = false
        }
    }

    fun stop() {
        runCatching { manager.removeOnActiveSessionsChangedListener(sessionsChanged) }
        controller?.unregisterCallback(callback)
        controller = null
    }

    fun playPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
        else c.transportControls.play()
    }

    /** Lance la lecture sur l'app choisie, sans l'ouvrir. */
    fun playChosen() {
        val pkg = Prefs.musicPackage.value ?: return
        val c = controller
        if (c != null && c.packageName == pkg) {
            c.transportControls.play()
            return
        }
        // L'app n'a pas de session ouverte : touche « lecture » envoyée directement à l'app
        for (action in listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
            ctx.sendBroadcast(
                Intent(Intent.ACTION_MEDIA_BUTTON)
                    .setPackage(pkg)
                    .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(action, KeyEvent.KEYCODE_MEDIA_PLAY))
            )
        }
    }

    fun next() {
        controller?.transportControls?.skipToNext()
    }

    fun previous() {
        controller?.transportControls?.skipToPrevious()
    }

    /** Relit les sessions (par exemple après un changement d'app de musique). */
    fun refresh() {
        try {
            pick(manager.getActiveSessions(listener))
        } catch (e: Exception) {
            _hasAccess.value = false
        }
    }

    private fun pick(sessions: List<MediaController>) {
        // L'app choisie d'abord ; une autre app seulement si elle joue vraiment.
        val chosen = Prefs.musicPackage.value
        val best = sessions.firstOrNull { it.packageName == chosen }
            ?: sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        if (best?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = best
            best?.registerCallback(callback, handler)
        }
        publish()
    }

    private fun publish() {
        val c = controller
        val meta = c?.metadata
        if (c == null || meta == null) {
            _track.value = null
            return
        }
        val state = c.playbackState
        _track.value = Track(
            title = meta.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: "Sans titre",
            artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) ?: "",
            appLabel = Launchers.label(ctx, c.packageName),
            packageName = c.packageName,
            art = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: meta.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON),
            playing = state?.state == PlaybackState.STATE_PLAYING,
            positionMs = state?.position ?: 0L,
            durationMs = meta.getLong(MediaMetadata.METADATA_KEY_DURATION),
            updatedAt = state?.lastPositionUpdateTime?.takeIf { it > 0 } ?: SystemClock.elapsedRealtime(),
            speed = state?.playbackSpeed?.takeIf { it > 0f } ?: 1f,
        )
    }

    companion object {
        fun hasAccess(ctx: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
    }
}
