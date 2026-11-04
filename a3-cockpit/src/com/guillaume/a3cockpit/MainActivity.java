package com.guillaume.a3cockpit;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity {
    static final String ACTION_DATA = "com.guillaume.a3cockpit.VEHICLE_DATA";
    private static final int REQ_PHOTO = 7, REQ_PERM = 8, REQ_CAM = 9;
    CameraView activeCamera;
    private boolean camAsked;

    final Handler h = new Handler(Looper.getMainLooper());
    private FrameLayout stage, content;
    private Dock dock;
    private HomePage home;
    private VehiclePage vehicle;
    private Page current;
    private float cw, ch;
    private int lastW, lastH;
    private Bitmap photo;
    private boolean photoLoaded;

    private GpsSource gps;
    private ObdSource obd;
    private DemoSource demo;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (dock != null) dock.refresh();
            if (current != null) current.refresh();
            h.postDelayed(this, 500);
        }
    };

    private final BroadcastReceiver ext = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { VehicleState.applyIntent(i); }
    };

    /** Racine qui recalcule l'échelle quand la taille de l'écran est connue. */
    private final class Root extends FrameLayout {
        Root(Context c) { super(c); }

        @Override protected void onSizeChanged(final int w, final int hh, int ow, int oh) {
            super.onSizeChanged(w, hh, ow, oh);
            if (w == lastW && hh == lastH) return;
            lastW = w;
            lastH = hh;
            post(new Runnable() { @Override public void run() { build(w, hh); } });
        }
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Prefs.init(this);
        Odometer.load();
        Maintenance.load();
        MediaHub.start(this);

        Root root = new Root(this);
        root.addView(new BackgroundView(this), new FrameLayout.LayoutParams(-1, -1));
        stage = new FrameLayout(this);
        root.addView(stage, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        gps = new GpsSource(this);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) gps.start();
        else requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_PERM);

        demo = new DemoSource(h);
        applyDemo();
        restartObd();
        registerReceiver(ext, new IntentFilter(ACTION_DATA));
    }

    @Override public void onRequestPermissionsResult(int req, String[] perms, int[] res) {
        if (req == REQ_PERM && res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) gps.start();
        if (req == REQ_CAM) {
            camAsked = false;
            if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED && activeCamera != null) activeCamera.resume();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        applyImmersive();
        MediaHub.refresh(this);
        if (current != null) current.onShow();
        if (activeCamera != null) activeCamera.resume();
        h.removeCallbacks(tick);
        h.post(tick);
    }

    void requestCameraPermission() {
        if (camAsked) return;
        camAsked = true;
        requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAM);
    }

    @Override protected void onPause() {
        super.onPause();
        h.removeCallbacks(tick);
        if (activeCamera != null) activeCamera.pause();   // libère la caméra pour l'appli de recul d'origine
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(ext); } catch (Exception ignored) { }
        gps.stop();
        if (obd != null) obd.shutdown();
        demo.setEnabled(false);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersive();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        showHome();
    }

    @Override public void onBackPressed() {
        if (current != home) showHome();
    }

    // ---------------- Construction de l'interface ----------------

    private void build(int w, int hh) {
        if (w <= 0 || hh <= 0) return;
        Ui.scale = Math.min(w / 1024f, hh / 600f);
        Ui.W = w / Ui.scale;
        Ui.H = hh / Ui.scale;
        int vehicleTab = vehicle != null ? vehicle.getTab() : 0;
        String page = current == null || current == home ? "home" : current instanceof VehiclePage ? "vehicle"
                : current instanceof AppsPage ? "apps" : current instanceof CameraPage ? "camera" : "settings";
        if (home != null) home.stopCamera();

        stage.removeAllViews();
        dock = Ui.place(stage, new Dock(this, Ui.H - 24), 12, 12, 68, Ui.H - 24);
        cw = Ui.W - 104;
        ch = Ui.H - 24;
        content = Ui.place(stage, new FrameLayout(this), 92, 12, cw, ch);
        home = new HomePage(this, cw, ch);
        vehicle = null;
        current = null;
        switch (page) {
            case "vehicle": showVehicle(vehicleTab); break;
            case "apps": showApps(); break;
            case "camera": showCamera(); break;
            case "settings": showSettings(); break;
            default: showHome();
        }
        dock.refresh();
    }

    private void show(Page p) {
        if (content == null || p == current) {
            if (p != null) p.onShow();
            return;
        }
        content.removeAllViews();
        content.addView(p, new FrameLayout.LayoutParams(-1, -1));
        current = p;
        p.setAlpha(0f);
        p.setTranslationY(Ui.f(10));
        p.animate().alpha(1f).translationY(0).setDuration(200).start();
        p.onShow();
    }

    void showHome() {
        if (home != null) show(home);
    }

    /** tab = -1 : garde l'onglet courant. */
    void showVehicle(int tab) {
        if (content == null) return;
        if (vehicle == null) vehicle = new VehiclePage(this, cw, ch);
        if (tab >= 0 && tab != vehicle.getTab()) vehicle.setTab(tab);
        show(vehicle);
    }

    void showApps() {
        if (content != null) show(new AppsPage(this, cw, ch));
    }

    void showCamera() {
        if (content != null) show(new CameraPage(this, cw, ch));
    }

    void showSettings() {
        if (content != null) show(new SettingsPage(this, cw, ch));
    }

    void refreshSoon() {
        h.postDelayed(new Runnable() { @Override public void run() { if (current != null) current.refresh(); } }, 300);
    }

    void openMediaApp() {
        if (MediaHub.pkg != null) {
            Intent i = getPackageManager().getLaunchIntentForPackage(MediaHub.pkg);
            if (i != null && Apps.start(this, i)) return;
        }
        Apps.open(this, Apps.MUSIC);
    }

    // ---------------- Réglages appliqués ----------------

    void applyImmersive() {
        View d = getWindow().getDecorView();
        if (Prefs.bool("fullscreen", false)) {
            d.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        } else {
            d.setSystemUiVisibility(0);
        }
    }

    void applyDemo() {
        demo.setEnabled(Prefs.bool("demo", false));
    }

    void restartObd() {
        if (obd != null) obd.shutdown();
        obd = null;
        String mac = Prefs.str("obd_mac", null);
        if (mac != null) {
            obd = new ObdSource(mac);
            obd.start();
        } else {
            VehicleState.obdStatus = "Désactivé";
            VehicleState.obdConnected = false;
        }
    }

    void carChanged() {
        if (home != null) home.reloadCar();
        if (vehicle != null) vehicle.setTab(vehicle.getTab());
    }

    // ---------------- Photo personnalisée ----------------

    Bitmap carPhoto() {
        if (!photoLoaded) {
            photoLoaded = true;
            File f = new File(getFilesDir(), "car.png");
            photo = Prefs.bool("car_photo", false) && f.exists() ? BitmapFactory.decodeFile(f.getAbsolutePath()) : null;
        }
        return photo;
    }

    void pickCarPhoto() {
        Toast.makeText(this, "Astuce : une photo de profil détourée (PNG transparent) rend le mieux.", Toast.LENGTH_LONG).show();
        Intent i = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(i, "Photo de la voiture"), REQ_PHOTO);
        } catch (Exception e) {
            Toast.makeText(this, "Aucune galerie disponible", Toast.LENGTH_SHORT).show();
        }
    }

    void clearCarPhoto() {
        Prefs.put("car_photo", false);
        new File(getFilesDir(), "car.png").delete();
        photo = null;
        photoLoaded = true;
        carChanged();
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_PHOTO || res != RESULT_OK || data == null || data.getData() == null) return;
        try {
            Uri u = data.getData();
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            InputStream in = getContentResolver().openInputStream(u);
            BitmapFactory.decodeStream(in, null, o);
            if (in != null) in.close();
            int sample = 1;
            while (o.outWidth / sample > 1600 || o.outHeight / sample > 1600) sample *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = sample;
            in = getContentResolver().openInputStream(u);
            Bitmap bmp = BitmapFactory.decodeStream(in, null, o2);
            if (in != null) in.close();
            if (bmp == null) throw new Exception();
            FileOutputStream out = new FileOutputStream(new File(getFilesDir(), "car.png"));
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();
            Prefs.put("car_photo", true);
            photo = bmp;
            photoLoaded = true;
            carChanged();
        } catch (Exception e) {
            Toast.makeText(this, "Impossible de lire cette image", Toast.LENGTH_SHORT).show();
        }
    }
}
