package com.guillaume.a3cockpit;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Carnet d'entretien : échéances au kilométrage et/ou à la date. */
final class Maintenance {
    private Maintenance() {}

    static final class Item {
        String name;
        int intervalKm;      // 0 = pas d'échéance kilométrique
        int intervalMonths;  // 0 = pas d'échéance de date
        long lastKm = -1;    // -1 = inconnu
        long lastDate = 0;   // 0 = inconnu (ms)

        Item(String name, int km, int months) {
            this.name = name;
            this.intervalKm = km;
            this.intervalMonths = months;
        }
    }

    static final List<Item> items = new ArrayList<>();

    static void load() {
        items.clear();
        String js = Prefs.str("maint", null);
        if (js != null) {
            try {
                JSONArray a = new JSONArray(js);
                for (int i = 0; i < a.length(); i++) {
                    JSONObject o = a.getJSONObject(i);
                    Item it = new Item(o.getString("n"), o.optInt("ik"), o.optInt("im"));
                    it.lastKm = o.optLong("lk", -1);
                    it.lastDate = o.optLong("ld", 0);
                    items.add(it);
                }
                return;
            } catch (Exception ignored) {
                items.clear();
            }
        }
        defaults();
    }

    static void defaults() {
        items.clear();
        boolean diesel = Prefs.diesel();
        items.add(new Item("Vidange + filtre à huile", 15000, 12));
        items.add(new Item("Révision (inspection)", 30000, 24));
        items.add(new Item("Filtre à air", 30000, 24));
        items.add(new Item("Filtre d'habitacle", 15000, 12));
        items.add(new Item(diesel ? "Filtre à gasoil" : "Bougies d'allumage", 60000, 48));
        items.add(new Item("Liquide de frein", 0, 24));
        items.add(new Item("Plaquettes de frein (contrôle)", 30000, 24));
        items.add(new Item("Courroie de distribution", 120000, 60));
        items.add(new Item("Contrôle technique", 0, 24));
        save();
    }

    static void save() {
        JSONArray a = new JSONArray();
        try {
            for (Item it : items) {
                JSONObject o = new JSONObject();
                o.put("n", it.name);
                o.put("ik", it.intervalKm);
                o.put("im", it.intervalMonths);
                o.put("lk", it.lastKm);
                o.put("ld", it.lastDate);
                a.put(o);
            }
        } catch (Exception ignored) { }
        Prefs.put("maint", a.toString());
    }

    // ---- Calculs --------------------------------------------------------------------------

    static boolean kmKnown(Item it) {
        return it.intervalKm > 0 && it.lastKm >= 0 && Odometer.known();
    }

    static boolean dateKnown(Item it) {
        return it.intervalMonths > 0 && it.lastDate > 0;
    }

    static long remainingKm(Item it) {
        return it.lastKm + it.intervalKm - Odometer.km();
    }

    static long dueDate(Item it) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(it.lastDate);
        c.add(Calendar.MONTH, it.intervalMonths);
        return c.getTimeInMillis();
    }

    static int remainingDays(Item it) {
        return (int) Math.floor((dueDate(it) - System.currentTimeMillis()) / 86400000d);
    }

    /** Fraction de l'intervalle déjà consommée (0..1+), -1 si inconnu. */
    static float used(Item it) {
        float u = -1;
        if (kmKnown(it)) u = Math.max(u, 1f - remainingKm(it) / (float) it.intervalKm);
        if (dateKnown(it)) u = Math.max(u, 1f - remainingDays(it) / (it.intervalMonths * 30.44f));
        return u;
    }

    static boolean overdue(Item it) {
        return (kmKnown(it) && remainingKm(it) < 0) || (dateKnown(it) && remainingDays(it) < 0);
    }

    static boolean soon(Item it) {
        if (overdue(it)) return false;
        return (kmKnown(it) && remainingKm(it) <= 1500) || (dateKnown(it) && remainingDays(it) <= 30);
    }

    static int color(Item it) {
        if (!kmKnown(it) && !dateKnown(it)) return Ui.TEXT3;
        if (overdue(it)) return Ui.RED;
        if (soon(it)) return Ui.AMBER;
        return Ui.GREEN;
    }

    static String dueText(Item it) {
        StringBuilder sb = new StringBuilder();
        boolean late = overdue(it);
        if (kmKnown(it)) {
            long r = remainingKm(it);
            sb.append(r >= 0 ? "Dans " + Ui.num(r) + " km" : "Dépassé de " + Ui.num(-r) + " km");
        }
        if (dateKnown(it)) {
            int d = remainingDays(it);
            if (sb.length() > 0) sb.append(" · ");
            else sb.append(d >= 0 ? "Dans " : "En retard de ");
            sb.append(duration(Math.abs(d)));
        }
        if (sb.length() == 0) return "À renseigner";
        return late && !sb.toString().startsWith("Dépassé") && !sb.toString().startsWith("En retard")
                ? "En retard · " + sb : sb.toString();
    }

    static String duration(int days) {
        if (days >= 60) return (days / 30) + " mois";
        if (days >= 14) return (days / 7) + " sem.";
        return days + (days > 1 ? " jours" : " jour");
    }

    static String subText(Item it) {
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);
        if (it.lastKm >= 0 || it.lastDate > 0) {
            StringBuilder sb = new StringBuilder("Fait");
            if (it.lastDate > 0) sb.append(" le ").append(df.format(new Date(it.lastDate)));
            if (it.lastKm >= 0) sb.append(" à ").append(Ui.num(it.lastKm)).append(" km");
            return sb.toString();
        }
        return "Pas encore renseigné · " + intervalText(it);
    }

    static String intervalText(Item it) {
        StringBuilder sb = new StringBuilder("tous les ");
        if (it.intervalKm > 0) sb.append(Ui.num(it.intervalKm)).append(" km");
        if (it.intervalMonths > 0) {
            if (it.intervalKm > 0) sb.append(" ou ");
            if (it.intervalMonths % 12 == 0) {
                int y = it.intervalMonths / 12;
                sb.append(y).append(y > 1 ? " ans" : " an");
            } else sb.append(it.intervalMonths).append(" mois");
        }
        return sb.toString();
    }

    /** L'élément le plus urgent parmi ceux renseignés. */
    static Item next() {
        Item best = null;
        float bestU = -1;
        for (Item it : items) {
            float u = used(it);
            if (u > bestU) { bestU = u; best = it; }
        }
        return best;
    }

    static String shortName(Item it) {
        String n = it.name;
        int p = n.indexOf(" (");
        if (p > 0) n = n.substring(0, p);
        p = n.indexOf(" +");
        if (p > 0) n = n.substring(0, p);
        return n;
    }
}
