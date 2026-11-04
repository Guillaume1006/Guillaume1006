package com.guillaume.a3cockpit;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;

/**
 * Les 7 emplacements du carrousel (orbite) de l'accueil. Chaque emplacement contient
 * une fonction intégrée ("fn:vehicle"…), une application ("app:paquet/activité") ou rien ("").
 */
final class OrbitSlots {
    private OrbitSlots() {}

    static final int COUNT = 7;
    /** Barre latérale : 5 applis maximum (+ bouton Menu fixe). */
    static final int SIDE_COUNT = 5;
    static final String[] DEFAULTS = {"fn:vehicle", "fn:radio", "fn:settings", "fn:music", "fn:phone", "fn:nav", "fn:menu"};
    static final String[] FN_IDS = {"vehicle", "radio", "music", "phone", "nav", "settings", "menu", "maintenance", "drawer"};
    static final String[] FN_LABELS = {"Véhicule", "Radio", "Musique", "Téléphone", "Navigation", "Réglages", "Menu", "Entretien", "Toutes les applis"};
    static final int[] FN_ICONS = {IconView.CAR, IconView.RADIO, IconView.MUSIC, IconView.PHONE, IconView.NAV, IconView.GEAR,
            IconView.GRID, IconView.WRENCH, IconView.GRID};

    static String get(int i) { return Prefs.str("orbit_" + i, DEFAULTS[i]); }

    static void set(int i, String v) { Prefs.put("orbit_" + i, v); }

    static void reset() { for (int i = 0; i < COUNT; i++) Prefs.remove("orbit_" + i); }

    private static int fnIndex(String v) {
        if (v == null || !v.startsWith("fn:")) return -1;
        String id = v.substring(3);
        for (int k = 0; k < FN_IDS.length; k++) if (FN_IDS[k].equals(id)) return k;
        return -1;
    }

    static Apps.App app(Context c, String v) {
        if (v == null || !v.startsWith("app:")) return null;
        ComponentName cn = ComponentName.unflattenFromString(v.substring(4));
        if (cn == null) return null;
        for (Apps.App a : Apps.list(c, false)) if (a.cn.equals(cn)) return a;
        return null;
    }

    static String label(Context c, String v) {
        int k = fnIndex(v);
        if (k >= 0) return FN_LABELS[k];
        Apps.App a = app(c, v);
        return a != null ? a.label : "";
    }

    static int icon(String v) {
        int k = fnIndex(v);
        return k >= 0 ? FN_ICONS[k] : IconView.GRID;
    }

    static Drawable appIcon(Context c, String v) {
        Apps.App a = app(c, v);
        return a != null ? a.icon : null;
    }

    static boolean empty(Context c, String v) {
        return v == null || v.isEmpty() || (fnIndex(v) < 0 && app(c, v) == null);
    }

    static void run(MainActivity a, String v) {
        int k = fnIndex(v);
        if (k >= 0) {
            switch (FN_IDS[k]) {
                case "vehicle": a.showVehicle(0); return;
                case "radio": Apps.open(a, Apps.RADIO); return;
                case "music": Apps.open(a, Apps.MUSIC); return;
                case "phone": Apps.open(a, Apps.PHONE); return;
                case "nav": Apps.open(a, Apps.NAV); return;
                case "settings": a.showSettings(); return;
                case "menu": a.showApps(); return;
                case "maintenance": a.showVehicle(3); return;
                default: a.showDrawer(); return;
            }
        }
        Apps.App app = app(a, v);
        if (app != null) Apps.launch(a, app.cn);
    }

    /** Dialogue de modification d'un emplacement. */
    static void edit(final MainActivity a, final int i, final Runnable done) {
        final String[] items = new String[FN_LABELS.length + 2];
        items[0] = "Choisir une application…";
        for (int k = 0; k < FN_LABELS.length; k++) items[k + 1] = "Fonction : " + FN_LABELS[k];
        items[items.length - 1] = "Retirer cet emplacement";
        String cur = label(a, get(i));
        new AlertDialog.Builder(a, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("Emplacement " + (i + 1) + (cur.isEmpty() ? "" : " · " + cur))
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        if (which == 0) {
                            Apps.pick(a, "Application de l'emplacement " + (i + 1), new Apps.Picked() {
                                @Override public void on(Apps.App app) {
                                    set(i, "app:" + app.cn.flattenToString());
                                    done.run();
                                }
                            });
                            return;
                        }
                        if (which == items.length - 1) set(i, "");
                        else set(i, "fn:" + FN_IDS[which - 1]);
                        done.run();
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }
}
