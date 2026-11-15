package com.guillaume.motodash

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.view.KeyEvent
import androidx.core.app.NotificationManagerCompat

/** Service vide : il sert uniquement à obtenir le droit de lire les sessions média. */
class MediaListenerService : NotificationListenerService()

data class MediaInfo(
    val access: Boolean,
    val title: String? = null,
    val artist: String? = null,
    val art: Bitmap? = null,
    val playing: Boolean = false,
    val pkg: String? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
)

object MediaRepo {
    fun hasAccess(ctx: Context) =
        NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)

    private fun controller(ctx: Context): MediaController? {
        if (!hasAccess(ctx)) return null
        return try {
            val msm = ctx.getSystemService(MediaSessionManager::class.java)
            val list = msm.getActiveSessions(ComponentName(ctx, MediaListenerService::class.java))
            list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list.firstOrNull()
        } catch (_: SecurityException) {
            null
        }
    }

    fun read(ctx: Context): MediaInfo {
        val c = controller(ctx) ?: return MediaInfo(access = hasAccess(ctx))
        val md = c.metadata
        val ps = c.playbackState
        val playing = ps?.state == PlaybackState.STATE_PLAYING
        val pos = when {
            ps == null -> 0L
            playing -> ps.position + ((SystemClock.elapsedRealtime() - ps.lastPositionUpdateTime) * ps.playbackSpeed).toLong()
            else -> ps.position
        }
        return MediaInfo(
            access = true,
            title = md?.getString(MediaMetadata.METADATA_KEY_TITLE),
            artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST),
            art = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART),
            playing = playing,
            pkg = c.packageName,
            positionMs = pos.coerceAtLeast(0),
            durationMs = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
        )
    }

    fun playPause(ctx: Context) {
        val c = controller(ctx)
        if (c == null) return key(ctx, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
        else c.transportControls.play()
    }

    fun next(ctx: Context) = controller(ctx)?.transportControls?.skipToNext() ?: key(ctx, KeyEvent.KEYCODE_MEDIA_NEXT)
    fun previous(ctx: Context) = controller(ctx)?.transportControls?.skipToPrevious() ?: key(ctx, KeyEvent.KEYCODE_MEDIA_PREVIOUS)

    fun volume(ctx: Context, up: Boolean) {
        val am = ctx.getSystemService(AudioManager::class.java)
        am.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI,
        )
    }

    /** Sans autorisation, on envoie de simples touches média : marche avec la plupart des lecteurs. */
    private fun key(ctx: Context, code: Int) {
        val am = ctx.getSystemService(AudioManager::class.java)
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }
}
