package com.guillaume.motodash

import android.content.ComponentName
import android.content.Context
import android.content.Intent
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

    /** Session média de l'appli musique choisie (ou, à défaut, celle qui joue). */
    private fun controller(ctx: Context, pkg: String?): MediaController? {
        if (!hasAccess(ctx)) return null
        return try {
            val msm = ctx.getSystemService(MediaSessionManager::class.java)
            val list = msm.getActiveSessions(ComponentName(ctx, MediaListenerService::class.java))
            list.firstOrNull { it.packageName == pkg }
                ?: list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        } catch (_: SecurityException) {
            null
        }
    }

    fun read(ctx: Context, pkg: String?): MediaInfo {
        val c = controller(ctx, pkg) ?: return MediaInfo(access = hasAccess(ctx))
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

    fun playPause(ctx: Context, pkg: String?) {
        val c = controller(ctx, pkg)
        if (c == null) return key(ctx, pkg, KeyEvent.KEYCODE_MEDIA_PLAY)
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
        else c.transportControls.play()
    }

    fun next(ctx: Context, pkg: String?) =
        controller(ctx, pkg)?.transportControls?.skipToNext() ?: key(ctx, pkg, KeyEvent.KEYCODE_MEDIA_NEXT)

    fun previous(ctx: Context, pkg: String?) =
        controller(ctx, pkg)?.transportControls?.skipToPrevious() ?: key(ctx, pkg, KeyEvent.KEYCODE_MEDIA_PREVIOUS)

    fun volume(ctx: Context, up: Boolean) {
        val am = ctx.getSystemService(AudioManager::class.java)
        am.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI,
        )
    }

    /**
     * Sans session active : on envoie la touche média directement à l'appli choisie
     * (réveille Spotify, Deezer… même à l'arrêt), sinon au lecteur actif.
     */
    private fun key(ctx: Context, pkg: String?, code: Int) {
        if (pkg != null) {
            for (action in listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
                val i = Intent(Intent.ACTION_MEDIA_BUTTON).setPackage(pkg)
                    .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(action, code))
                ctx.sendBroadcast(i)
            }
            return
        }
        val am = ctx.getSystemService(AudioManager::class.java)
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }
}
