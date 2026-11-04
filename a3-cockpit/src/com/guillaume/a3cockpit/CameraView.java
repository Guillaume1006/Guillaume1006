package com.guillaume.a3cockpit;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Size;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

/**
 * Image de la caméra de recul en direct, utilisable à tout moment (même en roulant).
 * Essaie l'API Camera2 puis l'ancienne API Camera, selon les réglages.
 */
final class CameraView extends FrameLayout implements TextureView.SurfaceTextureListener {
    private final MainActivity a;
    private final TextureView tv;
    private final Guides guides;
    private final TextView msg;
    private final Handler main = new Handler(Looper.getMainLooper());

    private boolean want;
    private SurfaceTexture st;
    private int pw = 720, ph = 480;
    private boolean gotFrame;

    // Camera2
    private CameraDevice dev;
    private CameraCaptureSession session;
    private HandlerThread bgThread;
    private Handler bg;
    // Camera1
    private Camera cam1;

    private final Runnable retry = new Runnable() {
        @Override public void run() { if (want && st != null) open(); }
    };

    CameraView(MainActivity a) {
        super(a);
        this.a = a;
        setBackgroundColor(0xFF000000);
        tv = new TextureView(a);
        tv.setSurfaceTextureListener(this);
        addView(tv, new LayoutParams(-1, -1));
        guides = new Guides(a);
        addView(guides, new LayoutParams(-1, -1));
        msg = Ui.text(a, "", 13, Ui.TEXT2, Ui.REG);
        msg.setSingleLine(false);
        msg.setMaxLines(6);
        msg.setGravity(Gravity.CENTER);
        msg.setPadding(Ui.s(16), 0, Ui.s(16), 0);
        addView(msg, new LayoutParams(-1, -1));
        Ui.onTap(msg, new Runnable() {
            @Override public void run() { if (!gotFrame) Apps.open(CameraView.this.a, Apps.CAM); }
        });
        msg.setClickable(false);
    }

    // ---------------- Cycle de vie ----------------

    void start() {
        want = true;
        a.activeCamera = this;
        guides.setVisibility(Prefs.bool("cam_guides", true) ? VISIBLE : GONE);
        if (st != null) open();
        else status("Démarrage de la caméra…", false);
    }

    void stop() {
        want = false;
        main.removeCallbacks(retry);
        close();
        if (a.activeCamera == this) a.activeCamera = null;
    }

    /** Libère la caméra (ex. appli en arrière-plan) sans oublier qu'elle doit tourner. */
    void pause() {
        main.removeCallbacks(retry);
        close();
    }

    void resume() {
        if (want && st != null) open();
    }

    boolean running() { return want; }

    @Override public void onSurfaceTextureAvailable(SurfaceTexture s, int w, int h) {
        st = s;
        if (want) open();
    }

    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture s, int w, int h) { transform(); }

    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture s) {
        close();
        st = null;
        return true;
    }

    @Override public void onSurfaceTextureUpdated(SurfaceTexture s) {
        if (!gotFrame) {
            gotFrame = true;
            msg.setVisibility(GONE);
        }
    }

    // ---------------- Ouverture ----------------

    private void open() {
        close();
        a.activeCamera = this;
        gotFrame = false;
        if (a.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            status("Autorisez l'accès à la caméra", false);
            a.requestCameraPermission();
            return;
        }
        status("Connexion à la caméra…", false);
        String api = Prefs.str("cam_api", "auto");
        if (!"camera1".equals(api) && openCamera2()) return;
        if (!"camera2".equals(api)) openCamera1();
        else fail("Camera2 indisponible sur cet autoradio. Essayez « Camera1 » dans Réglages.");
    }

    private boolean openCamera2() {
        try {
            CameraManager cm = (CameraManager) a.getSystemService(Context.CAMERA_SERVICE);
            if (cm == null) return false;
            String[] ids = cm.getCameraIdList();
            if (ids.length == 0) return false;
            String want = Prefs.str("cam_id", "auto");
            String id = ids[0];
            for (String s : ids) if (s.equals(want)) id = s;
            CameraCharacteristics ch = cm.getCameraCharacteristics(id);
            StreamConfigurationMap map = ch.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map != null) {
                Size best = null;
                Size[] sizes = map.getOutputSizes(SurfaceTexture.class);
                if (sizes != null) for (Size sz : sizes) {
                    if (sz.getWidth() > 1920) continue;
                    if (best == null || Math.abs(sz.getWidth() - 1280) < Math.abs(best.getWidth() - 1280)) best = sz;
                }
                if (best != null) { pw = best.getWidth(); ph = best.getHeight(); }
            }
            st.setDefaultBufferSize(pw, ph);
            main.post(new Runnable() { @Override public void run() { transform(); } });
            bgThread = new HandlerThread("camera");
            bgThread.start();
            bg = new Handler(bgThread.getLooper());
            cm.openCamera(id, new CameraDevice.StateCallback() {
                @Override public void onOpened(CameraDevice d) {
                    dev = d;
                    startSession();
                }

                @Override public void onDisconnected(CameraDevice d) {
                    d.close();
                    dev = null;
                    later("Caméra utilisée par une autre appli… nouvel essai", 3000);
                }

                @Override public void onError(CameraDevice d, int error) {
                    d.close();
                    dev = null;
                    fallbackOrRetry("Erreur Camera2 (" + error + ")");
                }
            }, bg);
            return true;
        } catch (CameraAccessException | SecurityException | IllegalArgumentException e) {
            return false;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void startSession() {
        try {
            if (dev == null || st == null) return;
            final Surface s = new Surface(st);
            final CaptureRequest.Builder b = dev.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            b.addTarget(s);
            List<Surface> outs = Arrays.asList(s);
            dev.createCaptureSession(outs, new CameraCaptureSession.StateCallback() {
                @Override public void onConfigured(CameraCaptureSession sess) {
                    session = sess;
                    try {
                        sess.setRepeatingRequest(b.build(), null, bg);
                    } catch (Exception e) {
                        fallbackOrRetry("Flux Camera2 refusé");
                    }
                }

                @Override public void onConfigureFailed(CameraCaptureSession sess) {
                    fallbackOrRetry("Configuration Camera2 impossible");
                }
            }, bg);
        } catch (Exception e) {
            fallbackOrRetry("Camera2 : " + e.getClass().getSimpleName());
        }
    }

    private void fallbackOrRetry(final String why) {
        main.post(new Runnable() {
            @Override public void run() {
                if (!want) return;
                closeCamera2();
                if ("auto".equals(Prefs.str("cam_api", "auto"))) openCamera1();
                else later(why + " · nouvel essai", 4000);
            }
        });
    }

    private void openCamera1() {
        try {
            int n = Camera.getNumberOfCameras();
            if (n == 0) {
                fail("Aucune caméra détectée.\nVérifiez le branchement, ou touchez ici pour ouvrir l'appli caméra de l'autoradio.");
                return;
            }
            int id = 0;
            try {
                int w = Integer.parseInt(Prefs.str("cam_id", "auto"));
                if (w >= 0 && w < n) id = w;
            } catch (NumberFormatException ignored) { }
            cam1 = Camera.open(id);
            Camera.Parameters p = cam1.getParameters();
            Camera.Size s = p.getPreviewSize();
            if (s != null) { pw = s.width; ph = s.height; }
            cam1.setPreviewTexture(st);
            cam1.startPreview();
            transform();
        } catch (Exception e) {
            if (cam1 != null) { try { cam1.release(); } catch (Exception ignored) { } cam1 = null; }
            later("Caméra indisponible (" + e.getClass().getSimpleName() + ").\nTouchez ici pour l'appli caméra de l'autoradio.", 4000);
        }
    }

    // ---------------- Affichage ----------------

    /** Ajuste l'image : entière (bandes noires) ou plein cadre, avec effet miroir optionnel. */
    private void transform() {
        float vw = tv.getWidth(), vh = tv.getHeight();
        if (vw <= 0 || vh <= 0 || pw <= 0 || ph <= 0) return;
        boolean fill = Prefs.bool("cam_fill", false);
        float s = fill ? Math.max(vw / pw, vh / ph) : Math.min(vw / pw, vh / ph);
        Matrix m = new Matrix();
        m.setScale(pw * s / vw, ph * s / vh, vw / 2f, vh / 2f);
        if (Prefs.bool("cam_mirror", false)) m.postScale(-1, 1, vw / 2f, vh / 2f);
        tv.setTransform(m);
    }

    private void status(String text, boolean error) {
        msg.setText(text);
        msg.setTextColor(error ? Ui.TEXT : Ui.TEXT2);
        msg.setVisibility(VISIBLE);
        msg.setClickable(error);
    }

    private void fail(String text) {
        status(text, true);
    }

    private void later(final String text, final long delay) {
        main.post(new Runnable() {
            @Override public void run() {
                if (!want) return;
                fail(text);
                main.removeCallbacks(retry);
                main.postDelayed(retry, delay);
            }
        });
    }

    private void closeCamera2() {
        try { if (session != null) session.close(); } catch (Exception ignored) { }
        session = null;
        try { if (dev != null) dev.close(); } catch (Exception ignored) { }
        dev = null;
        if (bgThread != null) bgThread.quitSafely();
        bgThread = null;
        bg = null;
    }

    private void close() {
        closeCamera2();
        if (cam1 != null) {
            try { cam1.stopPreview(); } catch (Exception ignored) { }
            try { cam1.release(); } catch (Exception ignored) { }
            cam1 = null;
        }
    }

    /** Liste des caméras vues par Android, pour les réglages. */
    static String describe(Context c) {
        StringBuilder sb = new StringBuilder();
        try {
            CameraManager cm = (CameraManager) c.getSystemService(Context.CAMERA_SERVICE);
            String[] ids = cm == null ? new String[0] : cm.getCameraIdList();
            sb.append("Camera2 : ").append(ids.length == 0 ? "aucune" : Arrays.toString(ids));
        } catch (Exception e) {
            sb.append("Camera2 : erreur");
        }
        try {
            sb.append("\nCamera1 : ").append(Camera.getNumberOfCameras()).append(" caméra(s)");
        } catch (Exception e) {
            sb.append("\nCamera1 : erreur");
        }
        return sb.toString();
    }

    static String[] ids(Context c) {
        try {
            CameraManager cm = (CameraManager) c.getSystemService(Context.CAMERA_SERVICE);
            String[] ids = cm.getCameraIdList();
            if (ids.length > 0) return ids;
        } catch (Exception ignored) { }
        try {
            int n = Camera.getNumberOfCameras();
            String[] out = new String[n];
            for (int i = 0; i < n; i++) out[i] = String.valueOf(i);
            return out;
        } catch (Exception e) {
            return new String[0];
        }
    }

    /** Lignes de guidage de stationnement (rouge / jaune / vert). */
    static final class Guides extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Guides(Context c) { super(c); }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeWidth(Math.max(2, w / 160f));
            // Trapèze : bas large (près du pare-chocs) → haut étroit (loin)
            float[] ys = {0.97f, 0.80f, 0.64f, 0.50f};
            int[] cols = {Ui.RED, Ui.YELLOW, Ui.GREEN};
            for (int i = 0; i < 3; i++) {
                float y0 = ys[i] * h, y1 = ys[i + 1] * h;
                float l0 = lx(ys[i]) * w, l1 = lx(ys[i + 1]) * w;
                p.setColor(Ui.alpha(cols[i], 0xE6));
                c.drawLine(l0, y0, l1, y1, p);
                c.drawLine(w - l0, y0, w - l1, y1, p);
                float tick = (w - 2 * l1) * 0.12f;
                c.drawLine(l1, y1, l1 + tick, y1, p);
                c.drawLine(w - l1, y1, w - l1 - tick, y1, p);
            }
        }

        private static float lx(float y) {
            // y = 0.97 → 0.12 ; y = 0.50 → 0.33
            return 0.12f + (0.97f - y) / 0.47f * 0.21f;
        }
    }
}
