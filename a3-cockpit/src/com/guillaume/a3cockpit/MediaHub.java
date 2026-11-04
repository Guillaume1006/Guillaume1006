package com.guillaume.a3cockpit;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.SystemClock;
import android.view.KeyEvent;

import java.util.List;

/** Titre en cours et commandes multimédia (Spotify, Deezer, radio, YouTube Music…). */
final class MediaHub {
    private MediaHub() {}

    static Context app;
    static MediaController ctl;
    static String title, artist, appLabel, pkg;
    static Bitmap art;
    static boolean playing;
    static boolean access;

    private static MediaSessionManager msm;
    private static ComponentName cn;
    private static MediaSessionManager.OnActiveSessionsChangedListener listener;

    private static final MediaController.Callback CB = new MediaController.Callback() {
        @Override public void onPlaybackStateChanged(PlaybackState state) { read(); }
        @Override public void onMetadataChanged(MediaMetadata metadata) { read(); }
        @Override public void onSessionDestroyed() { ctl = null; read(); }
    };

    static void start(Context c) {
        app = c.getApplicationContext();
        cn = new ComponentName(app, MediaListener.class);
        msm = (MediaSessionManager) app.getSystemService(Context.MEDIA_SESSION_SERVICE);
        refresh(app);
    }

    static void refresh(Context c) {
        if (msm == null) { if (app == null) start(c); return; }
        try {
            List<MediaController> list = msm.getActiveSessions(cn);
            access = true;
            if (listener == null) {
                listener = new MediaSessionManager.OnActiveSessionsChangedListener() {
                    @Override public void onActiveSessionsChanged(List<MediaController> controllers) { choose(controllers); }
                };
                msm.addOnActiveSessionsChangedListener(listener, cn);
            }
            choose(list);
        } catch (SecurityException e) {
            access = false;
        }
    }

    private static void choose(List<MediaController> list) {
        MediaController best = null;
        if (list != null) {
            for (MediaController m : list) {
                PlaybackState st = m.getPlaybackState();
                if (st != null && st.getState() == PlaybackState.STATE_PLAYING) { best = m; break; }
            }
            if (best == null && !list.isEmpty()) best = list.get(0);
        }
        if (ctl != null && best != null && ctl.getSessionToken().equals(best.getSessionToken())) { read(); return; }
        if (ctl != null) ctl.unregisterCallback(CB);
        ctl = best;
        if (ctl != null) ctl.registerCallback(CB);
        read();
    }

    private static void read() {
        if (ctl == null) {
            title = artist = null;
            art = null;
            playing = false;
            pkg = null;
            appLabel = null;
            return;
        }
        MediaMetadata md = ctl.getMetadata();
        if (md != null) {
            title = md.getString(MediaMetadata.METADATA_KEY_TITLE);
            if (title == null) title = md.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
            artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST);
            if (artist == null) artist = md.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE);
            Bitmap b = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
            if (b == null) b = md.getBitmap(MediaMetadata.METADATA_KEY_ART);
            if (b == null) b = md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
            art = b;
        } else {
            title = artist = null;
            art = null;
        }
        PlaybackState st = ctl.getPlaybackState();
        playing = st != null && st.getState() == PlaybackState.STATE_PLAYING;
        pkg = ctl.getPackageName();
        try {
            PackageManager pm = app.getPackageManager();
            appLabel = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception e) {
            appLabel = pkg;
        }
    }

    static void playPause() {
        if (ctl != null) {
            if (playing) ctl.getTransportControls().pause();
            else ctl.getTransportControls().play();
        } else key(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
    }

    static void next() {
        if (ctl != null) ctl.getTransportControls().skipToNext();
        else key(KeyEvent.KEYCODE_MEDIA_NEXT);
    }

    static void prev() {
        if (ctl != null) ctl.getTransportControls().skipToPrevious();
        else key(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
    }

    static float volumeFraction() {
        AudioManager am = app == null ? null : (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return 0;
        int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        return max <= 0 ? 0 : am.getStreamVolume(AudioManager.STREAM_MUSIC) / (float) max;
    }

    static void volume(int dir) {
        AudioManager am = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                dir > 0 ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
    }

    private static void key(int code) {
        AudioManager am = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return;
        long t = SystemClock.uptimeMillis();
        am.dispatchMediaKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_DOWN, code, 0));
        am.dispatchMediaKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_UP, code, 0));
    }
}
