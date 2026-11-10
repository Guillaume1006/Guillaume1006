package app.cadran;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * Coque native de Cadran : ouvre la liaison avec le boîtier ELM327 (Bluetooth SPP ou Wi-Fi TCP)
 * et relaie les octets vers la page, qui parle le protocole ELM327 elle-même.
 */
public class MainActivity extends Activity {
    private static final UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String PERM_CONNECT = "android.permission.BLUETOOTH_CONNECT";
    private static final String PERM_SCAN = "android.permission.BLUETOOTH_SCAN";
    private static final String PERM_FINE = "android.permission.ACCESS_FINE_LOCATION";
    private static final int REQ_PERM = 7;

    private WebView web;
    private final Object lock = new Object();
    private volatile int gen = 0;
    private Closeable link;
    private OutputStream out;
    private volatile boolean fullscreen;
    private final Handler main = new Handler(Looper.getMainLooper());
    private BroadcastReceiver scanRx;
    private BroadcastReceiver pairRx;
    private volatile String pairAddr;
    private String[] pairPins = new String[0];
    private int pairIdx;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        w.setStatusBarColor(0xFF0C0E0F);
        w.setNavigationBarColor(0xFF15181A);

        web = new WebView(this);
        web.setBackgroundColor(0xFF0C0E0F);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setSupportZoom(false);
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(), "ObdBridge");
        web.loadUrl("file:///android_asset/index.html");
        askPermission();
    }

    /** Plein écran immersif : barres d'état et de navigation masquées, réapparaissent brièvement par glissement. */
    private void applyFullscreen() {
        int flags = fullscreen
                ? View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                  | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                : 0;
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && fullscreen) applyFullscreen();
    }

    private boolean needsPermission() {
        return Build.VERSION.SDK_INT >= 31 && checkSelfPermission(PERM_CONNECT) != PackageManager.PERMISSION_GRANTED;
    }

    private void askPermission() {
        if (needsPermission()) requestPermissions(new String[]{PERM_CONNECT}, REQ_PERM);
    }

    private boolean granted(String p) {
        return checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED;
    }

    /** Autorisations nécessaires pour rechercher des appareils Bluetooth (la recherche classique exige la localisation). */
    private String[] scanPermissionsMissing() {
        ArrayList<String> l = new ArrayList<String>();
        if (Build.VERSION.SDK_INT >= 31) {
            if (!granted(PERM_SCAN)) l.add(PERM_SCAN);
            if (!granted(PERM_CONNECT)) l.add(PERM_CONNECT);
        }
        if (!granted(PERM_FINE)) {
            l.add(PERM_FINE);
            l.add("android.permission.ACCESS_COARSE_LOCATION");
        }
        return l.toArray(new String[0]);
    }

    private boolean locationOn() {
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            return lm == null || lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            return true;
        }
    }

    private static String deviceJson(String type, BluetoothDevice d, Intent i) {
        try {
            JSONObject o = new JSONObject();
            o.put("type", type);
            String name = null;
            try {
                name = d.getName();
            } catch (SecurityException ignored) {
            }
            if (name == null && i != null) name = i.getStringExtra(BluetoothDevice.EXTRA_NAME);
            o.put("name", name == null ? "" : name);
            o.put("address", d.getAddress());
            short rssi = i == null ? Short.MIN_VALUE : i.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
            if (rssi != Short.MIN_VALUE) o.put("rssi", rssi);
            int bond = BluetoothDevice.BOND_NONE;
            try {
                bond = d.getBondState();
            } catch (SecurityException ignored) {
            }
            o.put("bonded", bond == BluetoothDevice.BOND_BONDED);
            try {
                BluetoothClass bc = d.getBluetoothClass();
                o.put("major", bc == null ? -1 : bc.getMajorDeviceClass());
            } catch (SecurityException ignored) {
            }
            return o.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void registerScanReceiver() {
        if (scanRx != null) return;
        scanRx = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                String a = i.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(a) || BluetoothDevice.ACTION_NAME_CHANGED.equals(a)) {
                    BluetoothDevice d = i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (d == null) return;
                    String js = deviceJson("found", d, i);
                    if (js != null) emit("scan", js, null);
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(a)) {
                    emit("scan", "{\"type\":\"done\"}", null);
                }
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction(BluetoothDevice.ACTION_FOUND);
        f.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
        f.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(scanRx, f);
    }

    /** Appairage : répond aux demandes de code PIN avec la liste fournie, un code par tentative. */
    private void registerPairReceiver() {
        if (pairRx != null) return;
        pairRx = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                BluetoothDevice d = i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                final String addr = pairAddr;
                if (d == null || addr == null || !addr.equalsIgnoreCase(d.getAddress())) return;
                String a = i.getAction();
                if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(a)) {
                    int variant = i.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, -1);
                    if (variant == BluetoothDevice.PAIRING_VARIANT_PIN || variant == 7) {
                        String pin = pairPins[Math.min(pairIdx, pairPins.length - 1)];
                        try {
                            if (d.setPin(pin.getBytes("UTF-8")) && isOrderedBroadcast()) abortBroadcast();
                        } catch (Exception ignored) {
                        }
                    }
                } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(a)) {
                    int st = i.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                    int prev = i.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);
                    if (st == BluetoothDevice.BOND_BONDED) {
                        pairAddr = null;
                        emit("pair", "{\"type\":\"bonded\",\"address\":" + JSONObject.quote(addr) + "}", null);
                    } else if (st == BluetoothDevice.BOND_NONE && prev == BluetoothDevice.BOND_BONDING) {
                        pairIdx++;
                        if (pairIdx < pairPins.length) {
                            emit("pair", "{\"type\":\"progress\",\"pin\":" + JSONObject.quote(pairPins[pairIdx]) + "}", null);
                            final BluetoothDevice dev = d;
                            main.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        if (pairAddr != null) dev.createBond();
                                    } catch (SecurityException ignored) {
                                    }
                                }
                            }, 1500);
                        } else {
                            pairAddr = null;
                            emit("pair", "{\"type\":\"failed\"}", null);
                        }
                    }
                }
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        f.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        f.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);
        registerReceiver(pairRx, f);
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] res) {
        if (req == REQ_PERM) web.evaluateJavascript("window.obdNative&&obdNative.perm()", null);
    }

    private void emit(String fn, String a, String b) {
        final String js = "window.obdNative&&obdNative." + fn + "(" + JSONObject.quote(a) + "," + JSONObject.quote(b == null ? "" : b) + ")";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                web.evaluateJavascript(js, null);
            }
        });
    }

    private static String reason(Exception e) {
        String m = e.getMessage();
        return e.getClass().getSimpleName() + (m == null ? "" : " : " + m);
    }

    private void closeLink() {
        synchronized (lock) {
            if (link != null) {
                try {
                    link.close();
                } catch (IOException ignored) {
                }
            }
            link = null;
            out = null;
        }
    }

    /** Branche la liaison ouverte et lance la lecture continue des réponses du boîtier. */
    private void attach(final int g, Closeable l, final InputStream in, OutputStream o, String label) {
        synchronized (lock) {
            if (g != gen) {
                try {
                    l.close();
                } catch (IOException ignored) {
                }
                return;
            }
            link = l;
            out = o;
        }
        emit("state", "connected", label);
        byte[] buf = new byte[1024];
        try {
            while (g == gen) {
                int n = in.read(buf);
                if (n < 0) break;
                if (n > 0) emit("data", new String(buf, 0, n, "ISO-8859-1"), null);
            }
            if (g == gen) emit("state", "lost", "Fin de flux");
        } catch (IOException e) {
            if (g == gen) emit("state", "lost", reason(e));
        }
    }

    private BluetoothSocket openBt(BluetoothDevice d) throws Exception {
        BluetoothSocket s = null;
        try {
            s = d.createRfcommSocketToServiceRecord(SPP);
            s.connect();
            return s;
        } catch (IOException first) {
            if (s != null) try { s.close(); } catch (IOException ignored) { }
        }
        try {
            s = d.createInsecureRfcommSocketToServiceRecord(SPP);
            s.connect();
            return s;
        } catch (IOException second) {
            if (s != null) try { s.close(); } catch (IOException ignored) { }
        }
        // Dernier recours pour certains boîtiers clones : canal RFCOMM 1 direct.
        Method m = d.getClass().getMethod("createRfcommSocket", int.class);
        s = (BluetoothSocket) m.invoke(d, 1);
        s.connect();
        return s;
    }

    @Override
    protected void onDestroy() {
        try {
            if (scanRx != null) unregisterReceiver(scanRx);
            if (pairRx != null) unregisterReceiver(pairRx);
        } catch (Exception ignored) {
        }
        gen++;
        closeLink();
        web.destroy();
        super.onDestroy();
    }

    /** Méthodes appelées depuis la page. */
    private class Bridge {
        @JavascriptInterface
        public String devices() {
            try {
                if (needsPermission()) return "{\"error\":\"permission\"}";
                BluetoothAdapter ad = BluetoothAdapter.getDefaultAdapter();
                if (ad == null) return "{\"error\":\"Cet appareil n'a pas de Bluetooth.\"}";
                if (!ad.isEnabled()) return "{\"error\":\"Le Bluetooth est désactivé. Activez-le puis touchez Actualiser.\"}";
                JSONArray arr = new JSONArray();
                Set<BluetoothDevice> bonded = ad.getBondedDevices();
                if (bonded != null) {
                    for (BluetoothDevice d : bonded) {
                        JSONObject o = new JSONObject();
                        o.put("name", d.getName() == null ? "" : d.getName());
                        o.put("address", d.getAddress());
                        arr.put(o);
                    }
                }
                return new JSONObject().put("devices", arr).toString();
            } catch (SecurityException e) {
                return "{\"error\":\"permission\"}";
            } catch (Exception e) {
                return "{\"error\":" + JSONObject.quote("Lecture des appareils impossible (" + reason(e) + ")") + "}";
            }
        }

        @JavascriptInterface
        public void requestPermissions() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String[] missing = scanPermissionsMissing();
                    if (missing.length > 0) MainActivity.this.requestPermissions(missing, REQ_PERM);
                    else askPermission();
                }
            });
        }

        /** Lance la recherche. Renvoie ok, permission, location, btoff, nobt ou failed. */
        @JavascriptInterface
        public String startScan() {
            BluetoothAdapter ad = BluetoothAdapter.getDefaultAdapter();
            if (ad == null) return "nobt";
            if (!ad.isEnabled()) return "btoff";
            if (scanPermissionsMissing().length > 0) return "permission";
            if (!locationOn()) return "location";
            try {
                registerScanReceiver();
                registerPairReceiver();
                if (ad.isDiscovering()) ad.cancelDiscovery();
                return ad.startDiscovery() ? "ok" : "failed";
            } catch (SecurityException e) {
                return "permission";
            }
        }

        @JavascriptInterface
        public void stopScan() {
            try {
                BluetoothAdapter ad = BluetoothAdapter.getDefaultAdapter();
                if (ad != null && ad.isDiscovering()) ad.cancelDiscovery();
            } catch (SecurityException ignored) {
            }
        }

        /** Appaire le boîtier en essayant les codes donnés, séparés par des virgules. */
        @JavascriptInterface
        public String pair(String address, String pins) {
            try {
                BluetoothAdapter ad = BluetoothAdapter.getDefaultAdapter();
                if (ad == null || !ad.isEnabled()) return "btoff";
                if (needsPermission()) return "permission";
                try {
                    ad.cancelDiscovery();
                } catch (SecurityException ignored) {
                }
                BluetoothDevice d = ad.getRemoteDevice(address);
                registerPairReceiver();
                if (d.getBondState() == BluetoothDevice.BOND_BONDED) {
                    emit("pair", "{\"type\":\"bonded\",\"address\":" + JSONObject.quote(address) + "}", null);
                    return "ok";
                }
                ArrayList<String> l = new ArrayList<String>();
                for (String p : pins.split(",")) if (p.trim().length() > 0) l.add(p.trim());
                if (l.isEmpty()) l.add("1234");
                pairPins = l.toArray(new String[0]);
                pairIdx = 0;
                pairAddr = address;
                emit("pair", "{\"type\":\"progress\",\"pin\":" + JSONObject.quote(pairPins[0]) + "}", null);
                if (!d.createBond()) {
                    pairAddr = null;
                    return "failed";
                }
                return "ok";
            } catch (SecurityException e) {
                return "permission";
            } catch (Exception e) {
                return "failed";
            }
        }

        /** Ouvre un écran des réglages Android : location, bluetooth ou enableBt. */
        @JavascriptInterface
        public void openSettings(final String which) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if ("location".equals(which)) startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        else if ("enableBt".equals(which)) startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                        else startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                    } catch (Exception e) {
                        try {
                            startActivity(new Intent(Settings.ACTION_SETTINGS));
                        } catch (Exception ignored) {
                        }
                    }
                }
            });
        }

        @JavascriptInterface
        public void connectBt(final String address) {
            final int g = ++gen;
            closeLink();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        BluetoothAdapter ad = BluetoothAdapter.getDefaultAdapter();
                        if (ad == null || !ad.isEnabled()) throw new IOException("Bluetooth indisponible ou désactivé");
                        try {
                            ad.cancelDiscovery();
                        } catch (SecurityException ignored) {
                        }
                        BluetoothDevice d = ad.getRemoteDevice(address);
                        BluetoothSocket s = openBt(d);
                        String name = d.getName();
                        attach(g, s, s.getInputStream(), s.getOutputStream(), name == null ? address : name);
                    } catch (Exception e) {
                        if (g == gen) emit("state", "error", reason(e));
                    }
                }
            }, "cadran-bt").start();
        }

        @JavascriptInterface
        public void connectWifi(final String host, final int port) {
            final int g = ++gen;
            closeLink();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Socket s = new Socket();
                        s.connect(new InetSocketAddress(host, port), 7000);
                        s.setTcpNoDelay(true);
                        attach(g, s, s.getInputStream(), s.getOutputStream(), host + ":" + port);
                    } catch (Exception e) {
                        if (g == gen) emit("state", "error", reason(e));
                    }
                }
            }, "cadran-wifi").start();
        }

        @JavascriptInterface
        public void write(String cmd) {
            OutputStream o;
            synchronized (lock) {
                o = out;
            }
            if (o == null) return;
            try {
                o.write((cmd + "\r").getBytes("US-ASCII"));
                o.flush();
            } catch (IOException e) {
                emit("state", "lost", reason(e));
            }
        }

        @JavascriptInterface
        public void setFullscreen(boolean on) {
            fullscreen = on;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    applyFullscreen();
                }
            });
        }

        @JavascriptInterface
        public void disconnect() {
            gen++;
            closeLink();
        }
    }
}
