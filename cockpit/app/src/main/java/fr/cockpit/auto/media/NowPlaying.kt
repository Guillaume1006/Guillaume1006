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
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service vide : sa seule présence, une fois autorisée dans
 * Réglages > Accès aux notifications, ouvre l'accès aux sessions média des autres apps.
 */
class MediaListenerService : NotificationListenerService()

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
        } catch (e: SecurityException) {
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

    fun next() {
        controller?.transportControls?.skipToNext()
    }

    fun previous() {
        controller?.transportControls?.skipToPrevious()
    }

    private fun refresh() {
        try {
            pick(manager.getActiveSessions(listener))
        } catch (e: SecurityException) {
            _hasAccess.value = false
        }
    }

    private fun pick(sessions: List<MediaController>) {
        val best = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: sessions.firstOrNull()
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
            appLabel = appLabel(c.packageName),
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

    private fun appLabel(pkg: String): String = runCatching {
        val pm = ctx.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    companion object {
        fun hasAccess(ctx: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
    }
}
