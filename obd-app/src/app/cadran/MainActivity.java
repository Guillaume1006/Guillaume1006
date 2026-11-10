package app.cadran;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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
import java.util.Set;
import java.util.UUID;

/**
 * Coque native de Cadran : ouvre la liaison avec le boîtier ELM327 (Bluetooth SPP ou Wi-Fi TCP)
 * et relaie les octets vers la page, qui parle le protocole ELM327 elle-même.
 */
public class MainActivity extends Activity {
    private static final UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String PERM_CONNECT = "android.permission.BLUETOOTH_CONNECT";
    private static final int REQ_PERM = 7;

    private WebView web;
    private final Object lock = new Object();
    private volatile int gen = 0;
    private Closeable link;
    private OutputStream out;

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

    private boolean needsPermission() {
        return Build.VERSION.SDK_INT >= 31 && checkSelfPermission(PERM_CONNECT) != PackageManager.PERMISSION_GRANTED;
    }

    private void askPermission() {
        if (needsPermission()) requestPermissions(new String[]{PERM_CONNECT}, REQ_PERM);
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
                    askPermission();
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
        public void disconnect() {
            gen++;
            closeLink();
        }
    }
}
