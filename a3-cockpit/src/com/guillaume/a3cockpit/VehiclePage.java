package com.guillaume.a3cockpit;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Page Véhicule avec onglets Ouvrants / Radars / Ordi / Entretien. */
final class VehiclePage extends Page {
    static final String[] TABS = {"Ouvrants", "Radars", "Ordi", "Entretien"};

    private final TextView title, status;
    private final GradientDrawable dot;
    private final TextView[] tabViews = new TextView[4];
    private final FrameLayout body;
    private final float bw, bh;
    private int tab = 0;

    // contenu de l'onglet courant
    private View top;
    private final List<TextView> values = new ArrayList<>();
    private final List<IconView> icons = new ArrayList<>();
    private TextView big, foot, note;

    VehiclePage(final MainActivity a, float w, float h) {
        super(a, w, h);
        setBackground(Ui.card());

        FrameLayout back = Ui.place(this, new FrameLayout(a), 14, 18, 44, 44);
        back.setBackground(Ui.round(Ui.CHIP, 22));
        Ui.place(back, new IconView(a, IconView.BACK, Ui.TEXT), 11, 11, 22, 22);
        Ui.onTap(back, new Runnable() { @Override public void run() { a.showHome(); } });

        title = Ui.place(this, Ui.text(a, "Véhicule", 26, Ui.TEXT, Ui.BOLD), 70, 18, 240, 32);
        View dv = new View(a);
        dot = Ui.round(Ui.GRAY, 5);
        dv.setBackground(dot);
        Ui.place(this, dv, 71, 58, 9, 9);
        status = Ui.place(this, Ui.text(a, "", 13, Ui.TEXT2, Ui.REG), 86, 54, 260, 17);

        float tw = Math.min(460, w - 360), tx = w - tw - 20;
        FrameLayout seg = Ui.place(this, new FrameLayout(a), tx, 22, tw, 42);
        seg.setBackground(Ui.round(Ui.CHIP, 21));
        float sw = (tw - 8) / 4f;
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            TextView t = Ui.place(seg, Ui.text(a, TABS[i], 14, Ui.TEXT2, Ui.MED), 4 + i * sw, 4, sw, 34);
            t.setGravity(Gravity.CENTER);
            Ui.onTap(t, new Runnable() { @Override public void run() { setTab(idx); } });
            tabViews[i] = t;
        }

        bw = w;
        bh = h - 84;
        body = Ui.place(this, new FrameLayout(a), 0, 84, bw, bh);
        setTab(0);
    }

    void setTab(int t) {
        if (t < 0 || t > 3) return;
        tab = t;
        for (int i = 0; i < 4; i++) {
            tabViews[i].setBackground(i == t ? Ui.round(Ui.CHIP_ON, 17) : null);
            tabViews[i].setTextColor(i == t ? Ui.TEXT : Ui.TEXT2);
        }
        body.removeAllViews();
        values.clear();
        icons.clear();
        top = null;
        big = foot = note = null;
        switch (t) {
            case 0: buildOpenings(); break;
            case 1: buildRadar(); break;
            case 2: buildComputer(); break;
            default: buildMaintenance();
        }
        body.setAlpha(0f);
        body.animate().alpha(1f).setDuration(180).start();
        refresh();
    }

    int getTab() { return tab; }

    // ---------------- Ouvrants ----------------

    private void buildOpenings() {
        android.graphics.Bitmap img = a.openingsImage();
        float lw = bw * (img != null ? 0.5f : 0.44f);
        top = Ui.place(body, img != null ? new OpeningsView(a, img) : new CarTopView(a, CarTopView.MODE_OPENINGS),
                10, 0, lw, bh - 14);
        float x = lw + 20, rw = bw - x - 24;
        int[] list = VehicleState.relevantOpenings(Prefs.fiveDoors());
        float rowH = Math.min(58, (bh - 70) / list.length);
        for (int k = 0; k < list.length; k++) {
            int i = list[k];
            FrameLayout row = Ui.place(body, new FrameLayout(a), x, k * rowH, rw, rowH - 6);
            row.setBackground(Ui.round(Ui.CHIP, 14));
            int ic = i == VehicleState.TRUNK ? IconView.TRUNK : i == VehicleState.HOOD ? IconView.HOOD : IconView.DOOR;
            icons.add(Ui.place(row, new IconView(a, ic, Ui.TEXT2), 14, (rowH - 6 - 22) / 2f, 22, 22));
            Ui.place(row, Ui.text(a, VehicleState.openingName(i, Prefs.fiveDoors()), 16, Ui.TEXT, Ui.REG), 50, (rowH - 6 - 20) / 2f, rw - 150, 20);
            TextView v = Ui.place(row, Ui.text(a, "—", 15, Ui.TEXT2, Ui.MED), rw - 110, (rowH - 6 - 20) / 2f, 96, 20);
            v.setGravity(Gravity.END);
            values.add(v);
        }
        note = Ui.place(body, Ui.text(a, "", 12, Ui.TEXT3, Ui.REG), x, list.length * rowH + 6, rw, 50);
        note.setSingleLine(false);
        note.setMaxLines(3);
    }

    // ---------------- Radars ----------------

    private void buildRadar() {
        float lw = bw * 0.44f;
        top = Ui.place(body, new CarTopView(a, CarTopView.MODE_RADAR), 10, 0, lw, bh - 14);
        float x = lw + 20, rw = bw - x - 24;
        Ui.place(body, Ui.text(a, "Distance mini", 14, Ui.TEXT2, Ui.REG), x, 6, rw, 18);
        big = Ui.place(body, Ui.text(a, "—", 52, Ui.TEXT, Ui.MED), x, 26, rw, 64);
        String[] groups = {"Avant", "Arrière"};
        String[] pos = {"Gauche", "Centre G.", "Centre D.", "Droite"};
        float gy = 104;
        for (int g = 0; g < 2; g++) {
            Ui.place(body, Ui.text(a, groups[g], 13, Ui.TEXT3, Ui.MED), x, gy, rw, 16);
            float cw = (rw - 3 * 8) / 4f;
            for (int i = 0; i < 4; i++) {
                FrameLayout c = Ui.place(body, new FrameLayout(a), x + i * (cw + 8), gy + 22, cw, 62);
                c.setBackground(Ui.round(Ui.CHIP, 14));
                Ui.place(c, Ui.text(a, pos[i], 11, Ui.TEXT2, Ui.REG), 10, 10, cw - 14, 14);
                values.add(Ui.place(c, Ui.text(a, "—", 18, Ui.TEXT, Ui.MED), 10, 30, cw - 14, 22));
            }
            gy += 104;
        }
        note = Ui.place(body, Ui.text(a, "", 12, Ui.TEXT3, Ui.REG), x, gy + 4, rw, 50);
        note.setSingleLine(false);
        note.setMaxLines(3);
    }

    // ---------------- Ordinateur de bord ----------------

    private static final String[] PC_LABELS = {"Extérieur", "Batterie", "Kilométrage", "Carburant restant",
            "Régime moteur", "Vitesse", "Liquide de refroid.", "Air d'admission"};
    private static final int[] PC_ICONS = {IconView.THERMO, IconView.BATTERY, IconView.ROAD, IconView.FUEL,
            IconView.GAUGE, IconView.GAUGE, IconView.DROP, IconView.THERMO};
    private static final int[] PC_COLORS = {Ui.TEAL, Ui.GREEN, Ui.PURPLE, Ui.AMBER, Ui.PINK, Ui.BLUE, Ui.TEAL, Ui.LIME};

    private void buildComputer() {
        float pad = 20, gap = 12;
        float tw = (bw - 2 * pad - 3 * gap) / 4f, th = (bh - 46 - gap) / 2f;
        for (int i = 0; i < 8; i++) {
            float x = pad + (i % 4) * (tw + gap), y = (i / 4) * (th + gap);
            FrameLayout t = Ui.place(body, new FrameLayout(a), x, y, tw, th);
            t.setBackground(Ui.round(Ui.CHIP, 18));
            FrameLayout ic = Ui.place(t, new FrameLayout(a), 14, 14, 34, 34);
            ic.setBackground(Ui.round(Ui.alpha(PC_COLORS[i], 0x33), 10));
            Ui.place(ic, new IconView(a, PC_ICONS[i], PC_COLORS[i]), 7, 7, 20, 20);
            Ui.place(t, Ui.text(a, PC_LABELS[i], 13, Ui.TEXT2, Ui.REG), 14, th - 74, tw - 20, 16);
            values.add(Ui.place(t, Ui.text(a, "—", 34, Ui.TEXT, Ui.MED), 14, th - 52, tw - 20, 42));
            if (i == 2) Ui.onTap(t, new Runnable() { @Override public void run() { editOdometer(); } });
        }
        foot = Ui.place(body, Ui.text(a, "", 12, Ui.TEXT3, Ui.REG), pad, bh - 34, bw - 2 * pad, 16);
    }

    // ---------------- Entretien ----------------

    private void buildMaintenance() {
        float lw = Math.max(240, bw * 0.32f);
        FrameLayout left = Ui.place(body, new FrameLayout(a), 20, 0, lw, bh - 18);
        left.setBackground(Ui.round(Ui.CHIP, 18));
        Ui.place(left, Ui.text(a, Prefs.carName(), 13, Ui.TEXT2, Ui.REG), 18, 18, lw - 36, 16);
        Ui.place(left, Ui.text(a, "Kilométrage", 15, Ui.TEXT, Ui.MED), 18, 42, lw - 36, 19);
        big = Ui.place(left, Ui.text(a, "—", 34, Ui.TEXT, Ui.MED), 18, 68, lw - 36, 42);
        note = Ui.place(left, Ui.text(a, "", 12, Ui.TEXT3, Ui.REG), 18, 114, lw - 36, 15);

        FrameLayout edit = Ui.place(left, new FrameLayout(a), 18, 142, (lw - 46) / 2f, 40);
        edit.setBackground(Ui.round(Ui.CHIP_ON, 20));
        Ui.place(edit, new IconView(a, IconView.EDIT, Ui.TEXT), 12, 11, 18, 18);
        Ui.place(edit, Ui.text(a, "Modifier", 14, Ui.TEXT, Ui.MED), 36, 12, 80, 18);
        Ui.onTap(edit, new Runnable() { @Override public void run() { editOdometer(); } });

        FrameLayout fuel = Ui.place(left, new FrameLayout(a), 28 + (lw - 46) / 2f, 142, (lw - 46) / 2f, 40);
        fuel.setBackground(Ui.round(Ui.CHIP_ON, 20));
        final TextView ft = Ui.place(fuel, Ui.text(a, Prefs.diesel() ? "Diesel" : "Essence", 14, Ui.TEXT, Ui.MED), 0, 12, (lw - 46) / 2f, 18);
        ft.setGravity(Gravity.CENTER);
        Ui.onTap(fuel, new Runnable() {
            @Override public void run() {
                Prefs.put("fuel", Prefs.diesel() ? "essence" : "diesel");
                for (Maintenance.Item it : Maintenance.items) {
                    if (it.name.equals("Filtre à gasoil") && !Prefs.diesel()) it.name = "Bougies d'allumage";
                    else if (it.name.equals("Bougies d'allumage") && Prefs.diesel()) it.name = "Filtre à gasoil";
                }
                Maintenance.save();
                setTab(3);
            }
        });

        Ui.place(left, Ui.text(a, "Prochaine échéance", 12, Ui.TEXT2, Ui.REG), 18, 204, lw - 36, 15);
        foot = Ui.place(left, Ui.text(a, "", 16, Ui.TEXT, Ui.MED), 18, 224, lw - 36, 40);
        foot.setSingleLine(false);
        foot.setMaxLines(2);
        TextView hint = Ui.place(left, Ui.text(a, "Touchez une ligne pour la mettre à jour.\nAppui long pour la supprimer.",
                11, Ui.TEXT3, Ui.REG), 18, bh - 18 - 50, lw - 36, 34);
        hint.setSingleLine(false);
        hint.setMaxLines(2);

        float x = 20 + lw + 14, rw = bw - x - 20;
        ScrollView sv = Ui.place(body, new ScrollView(a), x, 0, rw, bh - 18);
        sv.setVerticalScrollBarEnabled(false);
        sv.setOverScrollMode(OVER_SCROLL_NEVER);
        LinearLayout ll = new LinearLayout(a);
        ll.setOrientation(LinearLayout.VERTICAL);
        sv.addView(ll);
        for (final Maintenance.Item it : Maintenance.items) {
            FrameLayout row = new FrameLayout(a);
            row.setBackground(Ui.round(Ui.CHIP, 16));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.s(rw), Ui.s(74));
            lp.bottomMargin = Ui.s(8);
            ll.addView(row, lp);
            int col = Maintenance.color(it);
            FrameLayout ic = Ui.place(row, new FrameLayout(a), 14, 18, 38, 38);
            ic.setBackground(Ui.round(Ui.alpha(col == Ui.TEXT3 ? Ui.GRAY : col, 0x33), 11));
            Ui.place(ic, new IconView(a, IconView.WRENCH, col == Ui.TEXT3 ? Ui.TEXT2 : col), 9, 9, 20, 20);
            float tx = 64, rightW = Math.min(190, rw * 0.36f);
            TextView n = Ui.place(row, Ui.text(a, it.name, 15, Ui.TEXT, Ui.MED), tx, 14, rw - tx - rightW - 10, 19);
            n.setMaxLines(1);
            Ui.place(row, Ui.text(a, Maintenance.subText(it), 12, Ui.TEXT2, Ui.REG), tx, 37, rw - tx - rightW - 10, 15);
            TextView due = Ui.place(row, Ui.text(a, Maintenance.dueText(it), 13, col == Ui.TEXT3 ? Ui.TEXT2 : (col == Ui.GREEN ? Ui.TEXT : col), Ui.MED),
                    rw - rightW - 14, 14, rightW, 36);
            due.setGravity(Gravity.END);
            due.setSingleLine(false);
            due.setMaxLines(2);
            float u = Maintenance.used(it);
            View track = Ui.place(row, new View(a), tx, 60, rw - tx - 14, 4);
            track.setBackground(Ui.round(0x1FFFFFFF, 2));
            if (u >= 0) {
                View bar = Ui.place(row, new View(a), tx, 60, Math.max(4, (rw - tx - 14) * Math.min(1f, Math.max(0f, u))), 4);
                bar.setBackground(Ui.round(col, 2));
            }
            Ui.onTap(row, new Runnable() { @Override public void run() { editItem(it); } });
            row.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) { deleteItem(it); return true; }
            });
        }
        FrameLayout add = new FrameLayout(a);
        add.setBackground(Ui.round(0x0FFFFFFF, 16));
        ll.addView(add, new LinearLayout.LayoutParams(Ui.s(rw), Ui.s(54)));
        Ui.place(add, new IconView(a, IconView.PLUS, Ui.TEXT2), 18, 16, 22, 22);
        Ui.place(add, Ui.text(a, "Ajouter un entretien", 15, Ui.TEXT2, Ui.MED), 52, 18, rw - 70, 19);
        Ui.onTap(add, new Runnable() { @Override public void run() { editItem(null); } });
    }

    // ---------------- Dialogues ----------------

    private EditText field(LinearLayout parent, String label, String value, boolean number) {
        TextView l = Ui.text(a, label, 13, Ui.TEXT2, Ui.REG);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = Ui.s(10);
        parent.addView(l, lp);
        EditText e = new EditText(a);
        e.setText(value);
        e.setTextColor(Ui.TEXT);
        e.setSingleLine(true);
        e.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, Ui.f(17));
        e.setInputType(number ? InputType.TYPE_CLASS_NUMBER : InputType.TYPE_CLASS_TEXT);
        parent.addView(e, new LinearLayout.LayoutParams(-1, -2));
        return e;
    }

    private LinearLayout form() {
        LinearLayout ll = new LinearLayout(a);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(Ui.s(24), Ui.s(4), Ui.s(24), Ui.s(4));
        return ll;
    }

    private void showForm(String titleText, LinearLayout ll, DialogInterface.OnClickListener ok, String neutral,
                          DialogInterface.OnClickListener neutralCb) {
        ScrollView sv = new ScrollView(a);
        sv.addView(ll);
        AlertDialog.Builder b = new AlertDialog.Builder(a, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle(titleText).setView(sv)
                .setPositiveButton("Enregistrer", ok)
                .setNegativeButton("Annuler", null);
        if (neutral != null) b.setNeutralButton(neutral, neutralCb);
        b.show();
    }

    void editOdometer() {
        LinearLayout ll = form();
        final EditText km = field(ll, "Kilométrage actuel (km)", Odometer.known() ? String.valueOf(Odometer.km()) : "", true);
        TextView help = Ui.text(a, "Il sera ensuite augmenté automatiquement grâce au GPS.", 12, Ui.TEXT3, Ui.REG);
        help.setSingleLine(false);
        ll.addView(help);
        showForm("Kilométrage", ll, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface d, int which) {
                try {
                    Odometer.setKm(Long.parseLong(km.getText().toString().trim()));
                } catch (NumberFormatException e) {
                    Toast.makeText(a, "Valeur invalide", Toast.LENGTH_SHORT).show();
                }
                setTab(tab);
            }
        }, null, null);
    }

    private void editItem(final Maintenance.Item existing) {
        final SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);
        final Maintenance.Item it = existing != null ? existing : new Maintenance.Item("", 15000, 12);
        LinearLayout ll = form();
        final EditText name = field(ll, "Nom", it.name, false);
        final EditText lastKm = field(ll, "Fait à (km)", it.lastKm >= 0 ? String.valueOf(it.lastKm) : "", true);
        final EditText lastDate = field(ll, "Fait le (jj/mm/aaaa)", it.lastDate > 0 ? df.format(new Date(it.lastDate)) : "", false);
        final EditText ik = field(ll, "Intervalle (km, 0 = aucun)", String.valueOf(it.intervalKm), true);
        final EditText im = field(ll, "Intervalle (mois, 0 = aucun)", String.valueOf(it.intervalMonths), true);
        showForm(existing != null ? it.name : "Nouvel entretien", ll, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface d, int which) {
                String n = name.getText().toString().trim();
                if (n.isEmpty()) return;
                it.name = n;
                it.lastKm = parseLong(lastKm.getText().toString(), -1);
                String ds = lastDate.getText().toString().trim();
                try {
                    it.lastDate = ds.isEmpty() ? 0 : df.parse(ds).getTime();
                } catch (ParseException e) {
                    Toast.makeText(a, "Date invalide (jj/mm/aaaa)", Toast.LENGTH_SHORT).show();
                }
                it.intervalKm = (int) parseLong(ik.getText().toString(), 0);
                it.intervalMonths = (int) parseLong(im.getText().toString(), 0);
                if (existing == null) Maintenance.items.add(it);
                Maintenance.save();
                setTab(3);
            }
        }, "Fait aujourd'hui", new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface d, int which) {
                String n = name.getText().toString().trim();
                if (!n.isEmpty()) it.name = n;
                if (it.name.isEmpty()) return;
                it.lastDate = System.currentTimeMillis();
                if (Odometer.known()) it.lastKm = Odometer.km();
                else if (!lastKm.getText().toString().trim().isEmpty()) it.lastKm = parseLong(lastKm.getText().toString(), -1);
                it.intervalKm = (int) parseLong(ik.getText().toString(), it.intervalKm);
                it.intervalMonths = (int) parseLong(im.getText().toString(), it.intervalMonths);
                if (existing == null) Maintenance.items.add(it);
                Maintenance.save();
                setTab(3);
            }
        });
    }

    private void deleteItem(final Maintenance.Item it) {
        new AlertDialog.Builder(a, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("Supprimer « " + it.name + " » ?")
                .setPositiveButton("Supprimer", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        Maintenance.items.remove(it);
                        Maintenance.save();
                        setTab(3);
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private static long parseLong(String s, long def) {
        try { return Long.parseLong(s.trim().replace(" ", "")); } catch (NumberFormatException e) { return def; }
    }

    // ---------------- Rafraîchissement ----------------

    @Override void refresh() {
        boolean five = Prefs.fiveDoors();
        status.setText(VehicleState.statusText(five));
        dot.setColor(VehicleState.statusColor(five));
        if (top != null) top.invalidate();
        switch (tab) {
            case 0: refreshOpenings(five); break;
            case 1: refreshRadar(); break;
            case 2: refreshComputer(); break;
            default: refreshMaintenance();
        }
    }

    private void refreshOpenings(boolean five) {
        int[] list = VehicleState.relevantOpenings(five);
        for (int k = 0; k < list.length && k < values.size(); k++) {
            int st = VehicleState.openings[list[k]];
            TextView v = values.get(k);
            v.setText(st == VehicleState.OPEN ? "Ouvert" : st == VehicleState.CLOSED ? "Fermé" : "—");
            v.setTextColor(st == VehicleState.OPEN ? Ui.RED : st == VehicleState.CLOSED ? Ui.TEXT2 : Ui.TEXT3);
            icons.get(k).set(iconFor(list[k]), st == VehicleState.OPEN ? Ui.RED : Ui.TEXT2);
        }
        if (note != null) {
            note.setText(VehicleState.openingsKnown(five)
                    ? "Source : " + VehicleState.openingsSource
                    : "Les ouvrants passent par le boîtier CAN de l'autoradio, non accessible aux applis. "
                    + "Activez le mode démo ou une source externe dans Réglages.");
        }
    }

    private static int iconFor(int i) {
        return i == VehicleState.TRUNK ? IconView.TRUNK : i == VehicleState.HOOD ? IconView.HOOD : IconView.DOOR;
    }

    private void refreshRadar() {
        boolean live = System.currentTimeMillis() - VehicleState.radarTime < 5000;
        int m = live ? VehicleState.radarMin() : -1;
        if (big != null) {
            big.setText(m >= 0 && m <= 150 ? Ui.valueUnit(String.valueOf(m), "cm") : VehicleState.radarActive() ? "—" : "En veille");
            big.setTextColor(m >= 0 && m <= 150 ? VehicleState.radarColor(m) : Ui.TEXT);
        }
        for (int i = 0; i < 8 && i < values.size(); i++) {
            int d = !live ? -1 : (i < 4 ? VehicleState.radarFront[i] : VehicleState.radarRear[i - 4]);
            TextView v = values.get(i);
            v.setText(d < 0 ? "—" : d > 150 ? "Libre" : d + " cm");
            v.setTextColor(d < 0 ? Ui.TEXT3 : d > 150 ? Ui.TEXT2 : VehicleState.radarColor(d));
        }
        if (note != null) {
            note.setText(live ? "Source : " + (VehicleState.openingsSource.isEmpty() ? "externe" : VehicleState.openingsSource)
                    : "Aucune donnée radar reçue. Les radars s'affichent ici en mode démo ou via une source externe.");
        }
    }

    private void refreshComputer() {
        if (values.size() < 8) return;
        set(0, VehicleState.ambient, 1, "°C");
        set(1, VehicleState.battery, 1, "V");
        values.get(2).setText(Odometer.known() ? Ui.valueUnit(Ui.num(Odometer.km()), "km") : "Toucher");
        set(3, VehicleState.fuelLiters(), 0, "L");
        set(4, VehicleState.rpm, 0, "tr/min");
        float sp = VehicleState.speed();
        set(5, sp < 0 ? Float.NaN : sp, 0, "km/h");
        set(6, VehicleState.coolant, 0, "°C");
        set(7, VehicleState.intake, 0, "°C");
        if (foot != null) {
            String t = VehicleState.dataTime > 0
                    ? "Mis à jour " + new SimpleDateFormat("HH:mm:ss", Locale.FRANCE).format(new Date(VehicleState.dataTime))
                    + " · " + VehicleState.dataSource
                    : "Aucune donnée moteur";
            foot.setText(t + "   ·   OBD2 : " + VehicleState.obdStatus + "   ·   GPS : "
                    + (VehicleState.gpsFix ? "actif" : "en attente") + "   ·   Km : " + Odometer.source);
        }
    }

    private void set(int i, float v, int decimals, String unit) {
        if (Float.isNaN(v)) {
            values.get(i).setText("—");
            values.get(i).setTextColor(Ui.TEXT3);
        } else {
            String s = decimals == 0 ? Ui.num(Math.round(v)) : Ui.dec1(v);
            values.get(i).setText(Ui.valueUnit(s, unit));
            values.get(i).setTextColor(Ui.TEXT);
        }
    }

    private void refreshMaintenance() {
        if (big != null) big.setText(Odometer.known() ? Ui.valueUnit(Ui.num(Odometer.km()), "km") : "À saisir");
        if (note != null) note.setText(Odometer.source);
        if (foot != null) {
            Maintenance.Item nx = Maintenance.next();
            foot.setText(nx == null ? "Renseignez vos derniers entretiens" : Maintenance.shortName(nx) + "\n" + Maintenance.dueText(nx));
            foot.setTextColor(nx == null ? Ui.TEXT2 : Maintenance.color(nx) == Ui.GREEN ? Ui.TEXT : Maintenance.color(nx));
        }
    }
}
