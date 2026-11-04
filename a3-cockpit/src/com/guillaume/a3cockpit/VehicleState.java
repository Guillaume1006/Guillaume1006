package com.guillaume.a3cockpit;

import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

/** État courant du véhicule, alimenté par le GPS, l'OBD2, le mode démo ou une diffusion externe. */
final class VehicleState {
    private VehicleState() {}

    static final int UNKNOWN = 0, CLOSED = 1, OPEN = 2;
    static final int FL = 0, FR = 1, RL = 2, RR = 3, TRUNK = 4, HOOD = 5;

    static final int[] openings = new int[6];
    static String openingsSource = "";

    /** Distances des radars en cm, -1 = inconnu. Ordre : gauche → droite. */
    static final int[] radarFront = {-1, -1, -1, -1};
    static final int[] radarRear = {-1, -1, -1, -1};
    static long radarTime;

    static volatile float gpsSpeed = -1;
    static volatile long gpsTime;
    static volatile boolean gpsFix;

    static volatile float obdSpeed = -1;
    static volatile long obdSpeedTime;
    static volatile float rpm = Float.NaN, coolant = Float.NaN, intake = Float.NaN, ambient = Float.NaN,
            battery = Float.NaN, fuelPct = Float.NaN, fuelL = Float.NaN, load = Float.NaN;
    static volatile long dataTime;
    static volatile String dataSource = "";

    static volatile boolean obdConnected;
    static volatile String obdStatus = "Désactivé";

    static float speed() {
        long now = System.currentTimeMillis();
        if (obdSpeed >= 0 && now - obdSpeedTime < 4000) return obdSpeed;
        if (gpsSpeed >= 0 && now - gpsTime < 5000) return gpsSpeed;
        return -1;
    }

    static float fuelLiters() {
        if (!Float.isNaN(fuelL)) return fuelL;
        if (!Float.isNaN(fuelPct)) return fuelPct * Prefs.tank() / 100f;
        return Float.NaN;
    }

    // ---- Ouvrants -------------------------------------------------------------------------

    static int[] relevantOpenings(boolean five) {
        return five ? new int[]{FL, FR, RL, RR, TRUNK, HOOD} : new int[]{FL, FR, TRUNK, HOOD};
    }

    static String openingName(int i, boolean five) {
        switch (i) {
            case FL: return five ? "Porte avant gauche" : "Porte gauche";
            case FR: return five ? "Porte avant droite" : "Porte droite";
            case RL: return "Porte arrière gauche";
            case RR: return "Porte arrière droite";
            case TRUNK: return "Coffre";
            default: return "Capot";
        }
    }

    static boolean openingsKnown(boolean five) {
        for (int i : relevantOpenings(five)) if (openings[i] != UNKNOWN) return true;
        return false;
    }

    static List<Integer> openList(boolean five) {
        List<Integer> l = new ArrayList<>();
        for (int i : relevantOpenings(five)) if (openings[i] == OPEN) l.add(i);
        return l;
    }

    static String statusText(boolean five) {
        if (!openingsKnown(five)) return "État des ouvrants indisponible";
        List<Integer> open = openList(five);
        if (open.isEmpty()) return "Tout est fermé";
        if (open.size() == 1) {
            int i = open.get(0);
            String n = openingName(i, five);
            return n + (i == TRUNK || i == HOOD ? " ouvert" : " ouverte");
        }
        return open.size() + " ouvrants ouverts";
    }

    static int statusColor(boolean five) {
        if (!openingsKnown(five)) return Ui.GRAY;
        return openList(five).isEmpty() ? Ui.GREEN : Ui.RED;
    }

    // ---- Radars ---------------------------------------------------------------------------

    static boolean radarActive() {
        if (System.currentTimeMillis() - radarTime > 5000) return false;
        for (int d : radarFront) if (d >= 0 && d < 150) return true;
        for (int d : radarRear) if (d >= 0 && d < 150) return true;
        return false;
    }

    static int radarMin() {
        int m = Integer.MAX_VALUE;
        for (int d : radarFront) if (d >= 0) m = Math.min(m, d);
        for (int d : radarRear) if (d >= 0) m = Math.min(m, d);
        return m == Integer.MAX_VALUE ? -1 : m;
    }

    static int radarColor(int d) {
        if (d < 0 || d > 150) return Ui.GREEN;
        if (d <= 35) return Ui.RED;
        if (d <= 70) return Ui.AMBER;
        if (d <= 110) return Ui.YELLOW;
        return Ui.GREEN;
    }

    // ---- Diffusion externe (Tasker, passerelle CAN, etc.) ---------------------------------

    static void applyIntent(Intent in) {
        Bundle b = in.getExtras();
        if (b == null) return;
        boolean any = false;
        String[] keys = {"door_fl", "door_fr", "door_rl", "door_rr", "trunk", "hood"};
        for (int i = 0; i < keys.length; i++) {
            if (b.containsKey(keys[i])) {
                Boolean v = asBool(b.get(keys[i]));
                if (v != null) { openings[i] = v ? OPEN : CLOSED; any = true; }
            }
        }
        if (any) openingsSource = "Diffusion externe";
        int[] f = asInts(b.get("radar_front"));
        int[] r = asInts(b.get("radar_rear"));
        if (f != null) { System.arraycopy(f, 0, radarFront, 0, Math.min(4, f.length)); radarTime = System.currentTimeMillis(); }
        if (r != null) { System.arraycopy(r, 0, radarRear, 0, Math.min(4, r.length)); radarTime = System.currentTimeMillis(); }

        Float v;
        if ((v = asFloat(b.get("speed"))) != null) { obdSpeed = v; obdSpeedTime = System.currentTimeMillis(); }
        if ((v = asFloat(b.get("rpm"))) != null) rpm = v;
        if ((v = asFloat(b.get("coolant"))) != null) coolant = v;
        if ((v = asFloat(b.get("intake"))) != null) intake = v;
        if ((v = asFloat(b.get("ambient"))) != null) ambient = v;
        if ((v = asFloat(b.get("battery"))) != null) battery = v;
        if ((v = asFloat(b.get("fuel_l"))) != null) fuelL = v;
        if ((v = asFloat(b.get("fuel_pct"))) != null) fuelPct = v;
        if ((v = asFloat(b.get("odometer"))) != null) Odometer.setFromObd(v);
        dataTime = System.currentTimeMillis();
        if (!obdConnected) dataSource = "Diffusion externe";
    }

    private static Boolean asBool(Object o) {
        if (o instanceof Boolean) return (Boolean) o;
        if (o instanceof Number) return ((Number) o).intValue() != 0;
        if (o != null) {
            String s = o.toString().trim().toLowerCase();
            if (s.equals("1") || s.equals("true") || s.equals("open") || s.equals("ouvert")) return true;
            if (s.equals("0") || s.equals("false") || s.equals("closed") || s.equals("ferme") || s.equals("fermé")) return false;
        }
        return null;
    }

    private static Float asFloat(Object o) {
        if (o instanceof Number) return ((Number) o).floatValue();
        if (o != null) {
            try { return Float.parseFloat(o.toString().trim().replace(',', '.')); } catch (NumberFormatException ignored) { }
        }
        return null;
    }

    private static int[] asInts(Object o) {
        if (o instanceof int[]) return (int[]) o;
        if (o instanceof String) {
            String[] parts = ((String) o).split("[,; ]+");
            int[] out = new int[parts.length];
            try {
                for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i].trim());
                return out;
            } catch (NumberFormatException e) { return null; }
        }
        return null;
    }
}
