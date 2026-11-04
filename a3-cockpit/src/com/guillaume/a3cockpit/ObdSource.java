package com.guillaume.a3cockpit;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lecture des PID OBD2 standards via un adaptateur ELM327 Bluetooth (profil série SPP). */
final class ObdSource extends Thread {
    private static final UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final Pattern VOLT = Pattern.compile("(\\d{1,2}[.,]\\d)");

    private final String mac;
    private volatile boolean running = true;
    private BluetoothSocket sock;
    private InputStream in;
    private OutputStream out;
    private final Map<String, Integer> misses = new HashMap<>();
    private int timeouts;
    private int cycle;

    ObdSource(String mac) {
        this.mac = mac;
        setDaemon(true);
        setName("obd2");
    }

    void shutdown() {
        running = false;
        close();
        interrupt();
    }

    @Override public void run() {
        while (running) {
            try {
                connect();
                init();
                VehicleState.obdConnected = true;
                loop();
            } catch (Exception e) {
                if (running) VehicleState.obdStatus = "Déconnecté · nouvel essai dans 10 s";
            }
            VehicleState.obdConnected = false;
            close();
            try { Thread.sleep(10000); } catch (InterruptedException e) { break; }
        }
        VehicleState.obdConnected = false;
        VehicleState.obdStatus = "Désactivé";
    }

    private void connect() throws Exception {
        BluetoothAdapter a = BluetoothAdapter.getDefaultAdapter();
        if (a == null) { VehicleState.obdStatus = "Pas de Bluetooth"; throw new IOException(); }
        if (!a.isEnabled()) { VehicleState.obdStatus = "Bluetooth désactivé"; throw new IOException(); }
        BluetoothDevice d = a.getRemoteDevice(mac);
        a.cancelDiscovery();
        VehicleState.obdStatus = "Connexion à " + Prefs.str("obd_name", mac) + "…";
        try {
            sock = d.createRfcommSocketToServiceRecord(SPP);
            sock.connect();
        } catch (IOException e) {
            close();
            sock = (BluetoothSocket) d.getClass().getMethod("createRfcommSocket", int.class).invoke(d, 1);
            sock.connect();
        }
        in = sock.getInputStream();
        out = sock.getOutputStream();
    }

    private void init() throws Exception {
        cmd("ATZ", 3000);
        Thread.sleep(400);
        cmd("ATE0", 1500);
        cmd("ATL0", 1500);
        cmd("ATS0", 1500);
        cmd("ATH0", 1500);
        cmd("ATAT1", 1500);
        cmd("ATSP0", 1500);
        VehicleState.obdStatus = "Recherche du protocole…";
        String r = clean(cmd("0100", 12000));
        VehicleState.obdStatus = r.contains("4100") ? "Connecté" : "ELM327 connecté · contact coupé ?";
    }

    private void loop() throws Exception {
        while (running) {
            poll("010D");
            poll("010C");
            if (cycle % 5 == 0) {
                poll("0105");
                poll("010F");
                poll("012F");
                poll("0104");
                poll("0146");
                voltage();
            }
            if (cycle % 60 == 0) poll("01A6");
            cycle++;
            Thread.sleep(120);
        }
    }

    private void poll(String pid) throws IOException, InterruptedException {
        Integer m = misses.get(pid);
        if (m != null && m >= 3 && cycle % 100 != 0) return;   // PID non supporté : on réessaie rarement
        String r = clean(cmd(pid, 2500));
        String key = "41" + pid.substring(2);
        int i = r.indexOf(key);
        if (i < 0) {
            misses.put(pid, m == null ? 1 : m + 1);
            return;
        }
        misses.remove(pid);
        if (!"Connecté".equals(VehicleState.obdStatus)) VehicleState.obdStatus = "Connecté";
        int[] b = bytes(r, i + key.length());
        if (b.length == 0) return;
        long now = System.currentTimeMillis();
        switch (pid) {
            case "010D": VehicleState.obdSpeed = b[0]; VehicleState.obdSpeedTime = now; break;
            case "010C": if (b.length >= 2) VehicleState.rpm = (b[0] * 256 + b[1]) / 4f; break;
            case "0105": VehicleState.coolant = b[0] - 40; break;
            case "010F": VehicleState.intake = b[0] - 40; break;
            case "0146": VehicleState.ambient = b[0] - 40; break;
            case "012F": VehicleState.fuelPct = b[0] * 100f / 255f; break;
            case "0104": VehicleState.load = b[0] * 100f / 255f; break;
            case "01A6":
                if (b.length >= 4) Odometer.setFromObd((((long) b[0] << 24) | (b[1] << 16) | (b[2] << 8) | b[3]) / 10.0);
                break;
            default:
        }
        VehicleState.dataTime = now;
        VehicleState.dataSource = "OBD2";
    }

    private void voltage() throws IOException, InterruptedException {
        Matcher m = VOLT.matcher(cmd("ATRV", 1500));
        if (m.find()) VehicleState.battery = Float.parseFloat(m.group(1).replace(',', '.'));
    }

    private String cmd(String c, long timeoutMs) throws IOException, InterruptedException {
        while (in.available() > 0) in.read();
        out.write((c + "\r").getBytes());
        out.flush();
        StringBuilder sb = new StringBuilder();
        long end = System.currentTimeMillis() + timeoutMs;
        boolean done = false;
        while (System.currentTimeMillis() < end) {
            if (in.available() > 0) {
                int ch = in.read();
                if (ch < 0) throw new IOException("closed");
                if (ch == '>') { done = true; break; }
                sb.append((char) ch);
            } else {
                Thread.sleep(8);
            }
        }
        if (!done) {
            if (++timeouts >= 6) throw new IOException("timeout");
        } else timeouts = 0;
        return sb.toString();
    }

    private static String clean(String s) {
        return s.toUpperCase().replace("SEARCHING...", "").replaceAll("[\\s\\r\\n]", "");
    }

    private static int[] bytes(String hex, int from) {
        int n = Math.min(4, (hex.length() - from) / 2);
        if (n <= 0) return new int[0];
        int[] out = new int[n];
        try {
            for (int i = 0; i < n; i++) out[i] = Integer.parseInt(hex.substring(from + 2 * i, from + 2 * i + 2), 16);
        } catch (NumberFormatException e) { return new int[0]; }
        return out;
    }

    private void close() {
        try { if (sock != null) sock.close(); } catch (IOException ignored) { }
        sock = null;
    }
}
