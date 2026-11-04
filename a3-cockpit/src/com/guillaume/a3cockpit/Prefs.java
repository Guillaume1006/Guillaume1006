package com.guillaume.a3cockpit;

import android.content.Context;
import android.content.SharedPreferences;

/** Réglages persistants de l'appli. */
final class Prefs {
    private Prefs() {}

    private static SharedPreferences sp;

    static void init(Context c) {
        if (sp == null) sp = c.getApplicationContext().getSharedPreferences("cockpit", Context.MODE_PRIVATE);
    }

    static String str(String k, String def) { return sp.getString(k, def); }
    static boolean bool(String k, boolean def) { return sp.getBoolean(k, def); }
    static int integer(String k, int def) { return sp.getInt(k, def); }
    static long lng(String k, long def) { return sp.getLong(k, def); }

    static void put(String k, String v) { sp.edit().putString(k, v).apply(); }
    static void put(String k, boolean v) { sp.edit().putBoolean(k, v).apply(); }
    static void put(String k, int v) { sp.edit().putInt(k, v).apply(); }
    static void put(String k, long v) { sp.edit().putLong(k, v).apply(); }
    static void remove(String k) { sp.edit().remove(k).apply(); }

    // Raccourcis
    static String carName() { return str("car_name", "Audi A3 Sportback"); }
    static boolean fiveDoors() { return "5".equals(str("car_body", "5")); }
    static int carColor() { return integer("car_color", 0xFF55595F); }
    static boolean diesel() { return !"essence".equals(str("fuel", "diesel")); }
    static int tank() { return integer("tank", 55); }
}
