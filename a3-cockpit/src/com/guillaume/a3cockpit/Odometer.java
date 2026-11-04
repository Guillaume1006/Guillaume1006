package com.guillaume.a3cockpit;

/**
 * Kilométrage : valeur saisie à la main, puis augmentée automatiquement avec la distance GPS
 * (ou remplacée par l'odomètre OBD si le véhicule le fournit).
 */
final class Odometer {
    private Odometer() {}

    private static long meters;      // 0 = inconnu
    private static long unsaved;     // mètres pas encore écrits
    static String source = "Saisie manuelle";

    static void load() {
        meters = Prefs.lng("odo_m", 0);
        source = Prefs.str("odo_src", "Saisie manuelle");
    }

    static boolean known() { return meters > 0; }

    static long km() { return meters / 1000; }

    static synchronized void setKm(long km) {
        meters = Math.max(0, km) * 1000;
        unsaved = 0;
        source = "Saisie manuelle";
        Prefs.put("odo_m", meters);
        Prefs.put("odo_src", source);
    }

    static synchronized void addMeters(double d) {
        if (meters <= 0 || d <= 0) return;      // tant que l'utilisateur n'a pas saisi de base, on n'invente rien
        meters += Math.round(d);
        unsaved += Math.round(d);
        if (!"Odomètre OBD".equals(source)) source = "Estimé par GPS";
        if (unsaved >= 100) {
            unsaved = 0;
            Prefs.put("odo_m", meters);
            Prefs.put("odo_src", source);
        }
    }

    static synchronized void setFromObd(double km) {
        if (km < 1) return;
        meters = Math.round(km * 1000);
        source = "Odomètre OBD";
        Prefs.put("odo_m", meters);
        Prefs.put("odo_src", source);
    }
}
