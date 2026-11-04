package com.guillaume.a3cockpit;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Réglages de l'appli. */
final class SettingsPage extends Page {
    private final LinearLayout list;
    private final float rw;
    private TextView obdValue;

    static final String[] COLOR_NAMES = {"Argent", "Gris clair", "Gris foncé", "Noir", "Blanc", "Rouge", "Bleu nuit",
            "Bleu vif", "Jaune", "Vert", "Orange", "Lime"};
    static final int[] COLORS = {0xFFC4C9CF, 0xFF9A9EA4, 0xFF4A4D52, 0xFF1C1D20, 0xFFEDEEF0, 0xFFC8102E, 0xFF22385E,
            0xFF1E5AD8, 0xFFF2C300, 0xFF3F6B3A, 0xFFE8641B, 0xFFC5D92B};

    SettingsPage(final MainActivity a, float w, float h) {
        super(a, w, h);
        setBackground(Ui.card());
        FrameLayout back = Ui.place(this, new FrameLayout(a), 14, 18, 44, 44);
        back.setBackground(Ui.round(Ui.CHIP, 22));
        Ui.place(back, new IconView(a, IconView.BACK, Ui.TEXT), 11, 11, 22, 22);
        Ui.onTap(back, new Runnable() { @Override public void run() { a.showHome(); } });
        Ui.place(this, Ui.text(a, "Réglages", 26, Ui.TEXT, Ui.BOLD), 70, 22, w - 100, 32);

        rw = w - 48;
        ScrollView sv = Ui.place(this, new ScrollView(a), 24, 78, rw, h - 90);
        sv.setVerticalScrollBarEnabled(false);
        list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list);
        build();
    }

    private void build() {
        list.removeAllViews();
        final MainActivity a = this.a;

        section("Véhicule");
        row(IconView.EDIT, "Nom affiché", Prefs.carName(), new Runnable() { @Override public void run() { editName(); } });
        row(IconView.CAR, "Carrosserie", Prefs.fiveDoors() ? "Sportback (5 portes)" : "3 portes", new Runnable() {
            @Override public void run() {
                Prefs.put("car_body", Prefs.fiveDoors() ? "3" : "5");
                a.carChanged();
                build();
            }
        });
        int ci = indexOf(Prefs.carColor());
        row(IconView.PALETTE, "Couleur", ci >= 0 ? COLOR_NAMES[ci] : "Personnalisée", new Runnable() { @Override public void run() { pickColor(); } });
        row(IconView.PHOTO, "Photo de ma voiture", Prefs.bool("car_photo", false) ? "Personnalisée · toucher pour retirer" : "Dessin intégré · toucher pour choisir",
                new Runnable() {
                    @Override public void run() {
                        if (Prefs.bool("car_photo", false)) { a.clearCarPhoto(); build(); }
                        else a.pickCarPhoto();
                    }
                });
        row(IconView.FUEL, "Réservoir", Prefs.tank() + " L", new Runnable() { @Override public void run() { editTank(); } });
        row(IconView.ROAD, "Kilométrage", Odometer.known() ? Ui.num(Odometer.km()) + " km" : "À saisir", new Runnable() {
            @Override public void run() { a.showVehicle(3); }
        });

        section("Données du véhicule");
        obdValue = row(IconView.BT, "Adaptateur OBD2 (ELM327 Bluetooth)",
                Prefs.str("obd_mac", null) == null ? "Aucun" : Prefs.str("obd_name", "") + " · " + VehicleState.obdStatus,
                new Runnable() { @Override public void run() { pickObd(); } });
        row(IconView.RADAR, "Mode démo (portes, radars, moteur)", Prefs.bool("demo", false) ? "Activé" : "Désactivé", new Runnable() {
            @Override public void run() {
                Prefs.put("demo", !Prefs.bool("demo", false));
                a.applyDemo();
                build();
            }
        });
        row(IconView.INFO, "Source externe", "Diffusion « com.guillaume.a3cockpit.VEHICLE_DATA »", new Runnable() {
            @Override public void run() { info(); }
        });

        section("Raccourcis");
        for (final String key : new String[]{Apps.NAV, Apps.MUSIC, Apps.PHONE, Apps.RADIO, Apps.SOUND}) {
            row(iconFor(key), Apps.role(key), Apps.currentLabel(a, key), new Runnable() {
                @Override public void run() {
                    Apps.pick(a, Apps.role(key), new Apps.Picked() {
                        @Override public void on(Apps.App app) {
                            Prefs.put(key, app.cn.flattenToString());
                            build();
                        }
                    });
                }
            });
        }
        row(IconView.BACK, "Réinitialiser les raccourcis", "Détection automatique", new Runnable() {
            @Override public void run() {
                for (String k : new String[]{Apps.NAV, Apps.MUSIC, Apps.PHONE, Apps.RADIO, Apps.SOUND}) Prefs.remove(k);
                build();
            }
        });

        section("Système");
        row(IconView.MUSIC, "Titres en cours de lecture", MediaHub.access ? "Accès autorisé" : "Autoriser l'accès aux notifications",
                new Runnable() {
                    @Override public void run() {
                        if (!Apps.start(a, new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")))
                            Toast.makeText(a, "Réglage introuvable sur cet autoradio", Toast.LENGTH_LONG).show();
                    }
                });
        row(IconView.HOME, "Écran d'accueil par défaut", "Choisir A3 Cockpit comme launcher", new Runnable() {
            @Override public void run() {
                if (!Apps.start(a, new Intent(Settings.ACTION_HOME_SETTINGS)))
                    Apps.start(a, new Intent(Settings.ACTION_SETTINGS));
            }
        });
        row(IconView.GRID, "Masquer les barres système", Prefs.bool("fullscreen", false) ? "Oui" : "Non", new Runnable() {
            @Override public void run() {
                Prefs.put("fullscreen", !Prefs.bool("fullscreen", false));
                a.applyImmersive();
                build();
            }
        });
        row(IconView.GEAR, "Paramètres Android", "", new Runnable() {
            @Override public void run() { Apps.start(a, new Intent(Settings.ACTION_SETTINGS)); }
        });
        row(IconView.INFO, "À propos", "A3 Cockpit 1.0", new Runnable() { @Override public void run() { info(); } });
    }

    private static int iconFor(String key) {
        switch (key) {
            case Apps.NAV: return IconView.NAV;
            case Apps.MUSIC: return IconView.MUSIC;
            case Apps.PHONE: return IconView.PHONE;
            case Apps.RADIO: return IconView.RADIO;
            default: return IconView.SOUND;
        }
    }

    private void section(String t) {
        TextView tv = Ui.text(a, t.toUpperCase(), 12, Ui.TEXT3, Ui.MED);
        tv.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = Ui.s(list.getChildCount() == 0 ? 0 : 18);
        lp.bottomMargin = Ui.s(8);
        lp.leftMargin = Ui.s(6);
        list.addView(tv, lp);
    }

    private TextView row(int icon, String title, String value, Runnable r) {
        FrameLayout row = new FrameLayout(a);
        row.setBackground(Ui.round(Ui.CHIP, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.s(rw), Ui.s(56));
        lp.bottomMargin = Ui.s(6);
        list.addView(row, lp);
        Ui.place(row, new IconView(a, icon, Ui.TEXT2), 16, 17, 22, 22);
        Ui.place(row, Ui.text(a, title, 16, Ui.TEXT, Ui.REG), 54, 18, rw * 0.45f, 20);
        TextView v = Ui.place(row, Ui.text(a, value, 14, Ui.TEXT2, Ui.REG), 54 + rw * 0.45f, 19, rw * 0.55f - 54 - 34, 18);
        v.setGravity(android.view.Gravity.END);
        Ui.place(row, new IconView(a, IconView.CHEVRON, Ui.TEXT3), rw - 30, 20, 16, 16);
        Ui.onTap(row, r);
        return v;
    }

    private int indexOf(int color) {
        for (int i = 0; i < COLORS.length; i++) if (COLORS[i] == color) return i;
        return -1;
    }

    private void editName() {
        final EditText e = new EditText(a);
        e.setText(Prefs.carName());
        e.setSingleLine(true);
        e.setTextColor(Ui.TEXT);
        FrameLayout wrap = new FrameLayout(a);
        wrap.setPadding(Ui.s(24), Ui.s(8), Ui.s(24), 0);
        wrap.addView(e);
        new AlertDialog.Builder(a, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("Nom affiché").setView(wrap)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        String s = e.getText().toString().trim();
                        if (!s.isEmpty()) Prefs.put("car_name", s);
                        build();
                    }
                })
                .setNegativeButton("Annuler", null).show();
    }

    private void editTank() {
        final EditText e = new EditText(a);
        e.setText(String.valueOf(Prefs.tank()));
        e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        e.setTextColor(Ui.TEXT);
        FrameLayout wrap = new FrameLayout(a);
        wrap.setPadding(Ui.s(24), Ui.s(8), Ui.s(24), 0);
        wrap.addView(e);
        new AlertDialog.Builder(a, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("Capacité du réservoir (L)").setView(wrap)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        try { Prefs.put("tank", Integer.parseInt(e.getText().toString().trim())); } catch (NumberFormatException ignored) { }
                        build();
                    }
                })
                .setNegativeButton("Annuler", null).show();
    }

    private void pickColor() {
        new AlertDialog.Builder(a, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("Couleur de la carrosserie")
                .setItems(COLOR_NAMES, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        Prefs.put("car_color", COLORS[which]);
                        a.carChanged();
                        build();
                    }
                })
                .setNegativeButton("Annuler", null).show();
    }

    private void pickObd() {
        BluetoothAdapter ad = BluetoothAdapter.getDefaultAdapter();
        if (ad == null) { Toast.makeText(a, "Bluetooth absent", Toast.LENGTH_SHORT).show(); return; }
        Set<BluetoothDevice> bonded;
        try { bonded = ad.getBondedDevices(); } catch (SecurityException e) { bonded = null; }
        final List<BluetoothDevice> devs = new ArrayList<>();
        List<String> names = new ArrayList<>();
        names.add("Aucun (désactiver)");
        if (bonded != null) for (BluetoothDevice d : bonded) {
            devs.add(d);
            String n = d.getName();
            names.add((n == null ? "Appareil" : n) + "  ·  " + d.getAddress());
        }
        new AlertDialog.Builder(a, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("Adaptateur OBD2 appairé")
                .setMessage(devs.isEmpty() ? "Appairez d'abord votre adaptateur ELM327 dans les réglages Bluetooth (code souvent 1234 ou 0000)." : null)
                .setItems(names.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        if (which == 0) {
                            Prefs.remove("obd_mac");
                            Prefs.remove("obd_name");
                        } else {
                            BluetoothDevice dev = devs.get(which - 1);
                            Prefs.put("obd_mac", dev.getAddress());
                            Prefs.put("obd_name", dev.getName() == null ? dev.getAddress() : dev.getName());
                        }
                        a.restartObd();
                        build();
                    }
                })
                .setNegativeButton("Annuler", null).show();
    }

    private void info() {
        TextView t = Ui.text(a, "", 14, Ui.TEXT2, Ui.REG);
        t.setSingleLine(false);
        t.setMaxLines(100);
        t.setPadding(Ui.s(24), Ui.s(8), Ui.s(24), Ui.s(8));
        t.setText("D'où viennent les données ?\n\n"
                + "• Vitesse et kilométrage : GPS de l'autoradio (le kilométrage part de la valeur que vous saisissez).\n"
                + "• Régime, températures, batterie, carburant : adaptateur OBD2 ELM327 Bluetooth branché sur la prise "
                + "diagnostic (sous le volant).\n"
                + "• Portes, coffre, capot, radars : ils transitent par votre boîtier CAN (HRD / LuZheng) vers le MCU de "
                + "l'autoradio, qui ne les partage pas avec les applis. Ils s'affichent en mode démo, ou si une autre appli "
                + "(Tasker, passerelle…) envoie la diffusion :\n\n"
                + "action : com.guillaume.a3cockpit.VEHICLE_DATA\n"
                + "extras : door_fl, door_fr, door_rl, door_rr, trunk, hood (true/false), radar_front, radar_rear "
                + "(4 distances en cm, ex. \"80,45,50,120\"), speed, rpm, coolant, intake, ambient, battery, fuel_l, "
                + "fuel_pct, odometer.");
        ScrollView sv = new ScrollView(a);
        sv.addView(t);
        new AlertDialog.Builder(a, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("A3 Cockpit").setView(sv).setPositiveButton("OK", null).show();
    }

    @Override void refresh() {
        if (obdValue != null && Prefs.str("obd_mac", null) != null)
            obdValue.setText(Prefs.str("obd_name", "") + " · " + VehicleState.obdStatus);
    }

    @Override void onShow() {
        build();
    }

}
