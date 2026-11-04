package com.guillaume.a3cockpit;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Écran d'accueil : voiture, multimédia, vitesse et raccourcis. */
final class HomePage extends Page {
    private TextView name, status, mediaSrc, mediaTitle, mediaArtist, date, speed, ext, batt;
    private View3 chipDoors, chipMaint, chipRadar;
    private ImageView art;
    private IconView artIcon, playIcon;
    private GradientDrawable dot;
    private View volBar;
    private float volW;
    final CarSideView car;

    /** Une puce d'état (icône + titre + valeur). */
    private static final class View3 {
        TextView value;
        IconView icon;
        FrameLayout box;
    }

    HomePage(final MainActivity a, float w, float h) {
        super(a, w, h);
        float carW = Math.round(w * 0.6f);
        float rx = carW + 12, rw = w - rx;

        // ---------- Carte véhicule ----------
        FrameLayout card = Ui.place(this, new FrameLayout(a), 0, 0, carW, h);
        card.setBackground(Ui.card());
        name = Ui.place(card, Ui.text(a, "", 27, Ui.TEXT, Ui.BOLD), 24, 22, carW - 200, 34);
        View dotV = new View(a);
        dot = Ui.round(Ui.GRAY, 5);
        dotV.setBackground(dot);
        Ui.place(card, dotV, 25, 64, 9, 9);
        status = Ui.place(card, Ui.text(a, "", 14, Ui.TEXT2, Ui.REG), 40, 60, carW - 80, 18);

        FrameLayout pill = Ui.place(card, new FrameLayout(a), carW - 146, 22, 124, 36);
        pill.setBackground(Ui.round(Ui.CHIP, 18));
        Ui.place(pill, new IconView(a, IconView.CAR, Ui.TEXT), 12, 8, 20, 20);
        Ui.place(pill, Ui.text(a, "Véhicule", 14, Ui.TEXT, Ui.MED), 38, 10, 70, 18);
        Ui.place(pill, new IconView(a, IconView.CHEVRON, Ui.TEXT2), 102, 11, 14, 14);
        Ui.onTap(pill, new Runnable() { @Override public void run() { a.showVehicle(-1); } });

        car = Ui.place(card, new CarSideView(a), 8, 86, carW - 16, h - 86 - 96);
        Ui.onTap(car, new Runnable() { @Override public void run() { car.shine(); } });

        float cw = (carW - 32 - 20) / 3f, cy = h - 84;
        chipDoors = chip(card, 16, cy, cw, IconView.DOOR, Ui.BLUE, "Ouvrants", 0);
        chipMaint = chip(card, 16 + cw + 10, cy, cw, IconView.WRENCH, Ui.AMBER, "Entretien", 3);
        chipRadar = chip(card, 16 + 2 * (cw + 10), cy, cw, IconView.RADAR, Ui.GREEN, "Radars", 1);

        // ---------- Carte multimédia ----------
        float mh = Math.round(h * 0.53f);
        FrameLayout media = Ui.place(this, new FrameLayout(a), rx, 0, rw, mh);
        media.setBackground(Ui.card());
        FrameLayout tile = Ui.place(media, new FrameLayout(a), 18, 18, 96, 96);
        tile.setBackground(Ui.gradient(0xFFFF7A3D, 0xFFE8361E, 22));
        tile.setClipToOutline(true);
        artIcon = Ui.place(tile, new IconView(a, IconView.RADIO, Ui.TEXT), 26, 26, 44, 44);
        art = Ui.place(tile, new ImageView(a), 0, 0, 96, 96);
        art.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Ui.onTap(tile, new Runnable() { @Override public void run() { a.openMediaApp(); } });

        mediaSrc = Ui.place(media, Ui.text(a, "", 13, Ui.TEXT2, Ui.REG), 130, 30, rw - 146, 16);
        mediaTitle = Ui.place(media, Ui.text(a, "", 22, Ui.TEXT, Ui.BOLD), 130, 52, rw - 146, 28);
        mediaArtist = Ui.place(media, Ui.text(a, "", 14, Ui.TEXT2, Ui.REG), 130, 86, rw - 146, 18);

        // Volume
        float vy = Math.max(124, (118 + mh - 92) / 2f - 4);
        FrameLayout vminus = Ui.place(media, new FrameLayout(a), 18, vy, 40, 36);
        vminus.setBackground(Ui.round(Ui.CHIP, 18));
        TextView tm = Ui.place(vminus, Ui.text(a, "−", 22, Ui.TEXT, Ui.MED), 0, 4, 40, 28);
        tm.setGravity(Gravity.CENTER);
        Ui.onTap(vminus, new Runnable() { @Override public void run() { MediaHub.volume(-1); refresh(); } });
        FrameLayout vplus = Ui.place(media, new FrameLayout(a), rw - 58, vy, 40, 36);
        vplus.setBackground(Ui.round(Ui.CHIP, 18));
        TextView tp = Ui.place(vplus, Ui.text(a, "+", 22, Ui.TEXT, Ui.MED), 0, 4, 40, 28);
        tp.setGravity(Gravity.CENTER);
        Ui.onTap(vplus, new Runnable() { @Override public void run() { MediaHub.volume(1); refresh(); } });
        volW = rw - 66 - 66;
        View track = Ui.place(media, new View(a), 66, vy + 16, volW, 5);
        track.setBackground(Ui.round(0x26FFFFFF, 3));
        volBar = Ui.place(media, new View(a), 66, vy + 16, volW, 5);
        volBar.setBackground(Ui.round(Ui.TEXT, 3));
        volBar.setPivotX(0);

        float ctrlY = mh - 92, mid = rw / 2f;
        FrameLayout prev = Ui.place(media, new FrameLayout(a), mid - 122, ctrlY + 8, 56, 56);
        Ui.place(prev, new IconView(a, IconView.PREV, Ui.TEXT), 14, 14, 28, 28);
        Ui.onTap(prev, new Runnable() { @Override public void run() { MediaHub.prev(); } });
        FrameLayout play = Ui.place(media, new FrameLayout(a), mid - 36, ctrlY, 72, 72);
        play.setBackground(Ui.round(0x33FFFFFF, 36));
        playIcon = Ui.place(play, new IconView(a, IconView.PLAY, Ui.TEXT), 20, 20, 32, 32);
        Ui.onTap(play, new Runnable() { @Override public void run() { MediaHub.playPause(); a.refreshSoon(); } });
        FrameLayout next = Ui.place(media, new FrameLayout(a), mid + 66, ctrlY + 8, 56, 56);
        Ui.place(next, new IconView(a, IconView.NEXT, Ui.TEXT), 14, 14, 28, 28);
        Ui.onTap(next, new Runnable() { @Override public void run() { MediaHub.next(); } });

        // ---------- Infos de conduite ----------
        float by = mh + 12, bh = h - by, iw = (rw - 12) / 2f;
        FrameLayout info = Ui.place(this, new FrameLayout(a), rx, by, iw, bh);
        info.setBackground(Ui.card());
        date = Ui.place(info, Ui.text(a, "", 13, Ui.TEXT2, Ui.REG), 16, 16, iw - 24, 16);
        speed = Ui.place(info, Ui.text(a, "0", 58, Ui.TEXT, Ui.MED), 10, 36, iw - 20, 70);
        speed.setGravity(Gravity.CENTER);
        Ui.place(info, Ui.text(a, "km/h", 13, Ui.TEXT2, Ui.REG), 0, 108, iw, 16).setGravity(Gravity.CENTER);
        float my = bh - 62, mw = (iw - 32 - 8) / 2f;
        ext = mini(info, 16, my, mw, "Ext.");
        batt = mini(info, 16 + mw + 8, my, mw, "Batt.");

        // ---------- Raccourcis ----------
        FrameLayout tiles = Ui.place(this, new FrameLayout(a), rx + iw + 12, by, iw, bh);
        tiles.setBackground(Ui.card());
        float tw = (iw - 16 * 2 - 12) / 2f, th = (bh - 16 * 2 - 12) / 2f;
        shortcut(tiles, 16, 16, tw, th, IconView.RADIO, 0xFFFF7A3D, 0xFFE8361E, "Radio", new Runnable() {
            @Override public void run() { Apps.open(a, Apps.RADIO); }
        });
        shortcut(tiles, 16 + tw + 12, 16, tw, th, IconView.SOUND, 0xFF9C86FF, 0xFF5B3FE0, "Son", new Runnable() {
            @Override public void run() { Apps.open(a, Apps.SOUND); }
        });
        shortcut(tiles, 16, 16 + th + 12, tw, th, IconView.CAR, 0xFFD2E85A, 0xFF8FAE14, "Véhicule", new Runnable() {
            @Override public void run() { a.showVehicle(2); }
        });
        shortcut(tiles, 16 + tw + 12, 16 + th + 12, tw, th, IconView.GEAR, 0xFF8AD8FF, 0xFF2F9BD6, "Réglages", new Runnable() {
            @Override public void run() { a.showSettings(); }
        });

        reloadCar();
    }

    void reloadCar() {
        car.set(Prefs.carColor(), Prefs.fiveDoors(), a.carPhoto());
    }

    private View3 chip(FrameLayout parent, float x, float y, float w, int icon, int color, String title, final int tab) {
        View3 v = new View3();
        v.box = Ui.place(parent, new FrameLayout(a), x, y, w, 66);
        v.box.setBackground(Ui.round(Ui.CHIP, 18));
        FrameLayout ic = Ui.place(v.box, new FrameLayout(a), 12, 15, 36, 36);
        ic.setBackground(Ui.round(Ui.alpha(color, 0x33), 11));
        v.icon = Ui.place(ic, new IconView(a, icon, color), 8, 8, 20, 20);
        Ui.place(v.box, Ui.text(a, title, 12, Ui.TEXT2, Ui.REG), 58, 15, w - 66, 15);
        v.value = Ui.place(v.box, Ui.text(a, "—", 15, Ui.TEXT, Ui.MED), 58, 33, w - 66, 19);
        Ui.onTap(v.box, new Runnable() { @Override public void run() { a.showVehicle(tab); } });
        return v;
    }

    private TextView mini(FrameLayout parent, float x, float y, float w, String label) {
        FrameLayout b = Ui.place(parent, new FrameLayout(a), x, y, w, 46);
        b.setBackground(Ui.round(Ui.CHIP, 12));
        Ui.place(b, Ui.text(a, label, 11, Ui.TEXT2, Ui.REG), 9, 7, w - 14, 13);
        return Ui.place(b, Ui.text(a, "—", 15, Ui.TEXT, Ui.MED), 9, 23, w - 12, 18);
    }

    private void shortcut(FrameLayout parent, float x, float y, float w, float h, int icon, int c1, int c2, String label, Runnable r) {
        FrameLayout b = Ui.place(parent, new FrameLayout(a), x, y, w, h);
        float sz = Math.min(48, Math.min(w, h - 22));
        FrameLayout ic = Ui.place(b, new FrameLayout(a), (w - sz) / 2f, 2, sz, sz);
        ic.setBackground(Ui.gradient(c1, c2, sz * 0.3f));
        Ui.place(ic, new IconView(a, icon, Ui.TEXT), sz * 0.22f, sz * 0.22f, sz * 0.56f, sz * 0.56f);
        TextView t = Ui.place(b, Ui.text(a, label, 12, Ui.TEXT, Ui.REG), 0, sz + 7, w, 15);
        t.setGravity(Gravity.CENTER);
        Ui.onTap(b, r);
    }

    @Override void refresh() {
        boolean five = Prefs.fiveDoors();
        name.setText(Prefs.carName());
        status.setText(VehicleState.statusText(five));
        dot.setColor(VehicleState.statusColor(five));

        // Ouvrants
        if (!VehicleState.openingsKnown(five)) {
            chipDoors.value.setText("Indisponible");
            chipDoors.value.setTextColor(Ui.TEXT3);
        } else {
            List<Integer> open = VehicleState.openList(five);
            chipDoors.value.setText(open.isEmpty() ? "Fermés" : open.size() == 1
                    ? VehicleState.openingName(open.get(0), five).replace("Porte ", "") : open.size() + " ouverts");
            chipDoors.value.setTextColor(open.isEmpty() ? Ui.TEXT : Ui.RED);
        }
        // Entretien
        Maintenance.Item nx = Maintenance.next();
        if (nx == null) {
            chipMaint.value.setText("À renseigner");
            chipMaint.value.setTextColor(Ui.TEXT3);
        } else if (Maintenance.overdue(nx)) {
            chipMaint.value.setText(Maintenance.shortName(nx) + " !");
            chipMaint.value.setTextColor(Ui.RED);
        } else if (Maintenance.soon(nx)) {
            chipMaint.value.setText(Maintenance.shortName(nx));
            chipMaint.value.setTextColor(Ui.AMBER);
        } else {
            chipMaint.value.setText("Contrôlé");
            chipMaint.value.setTextColor(Ui.TEXT);
        }
        // Radars
        if (VehicleState.radarActive()) {
            int m = VehicleState.radarMin();
            chipRadar.value.setText(m + " cm");
            chipRadar.value.setTextColor(VehicleState.radarColor(m));
        } else {
            chipRadar.value.setText("En veille");
            chipRadar.value.setTextColor(Ui.TEXT);
        }

        // Multimédia
        if (MediaHub.ctl != null) {
            mediaSrc.setText(MediaHub.appLabel != null ? MediaHub.appLabel : "Multimédia");
            mediaTitle.setText(MediaHub.title != null ? MediaHub.title : (MediaHub.appLabel != null ? MediaHub.appLabel : "Lecture"));
            mediaArtist.setText(MediaHub.artist != null ? MediaHub.artist : "");
        } else {
            mediaSrc.setText("Radio");
            mediaTitle.setText(MediaHub.access ? "Rien en lecture" : "Multimédia");
            mediaArtist.setText(MediaHub.access ? "Touchez l'icône pour ouvrir" : "Autorisez l'accès aux notifications (Réglages)");
        }
        if (MediaHub.art != null) {
            art.setImageBitmap(MediaHub.art);
            art.setVisibility(VISIBLE);
        } else {
            art.setImageDrawable(null);
            art.setVisibility(GONE);
        }
        artIcon.set(MediaHub.pkg != null && !MediaHub.pkg.contains("radio") ? IconView.MUSIC : IconView.RADIO, Ui.TEXT);
        playIcon.set(MediaHub.playing ? IconView.PAUSE : IconView.PLAY, Ui.TEXT);

        float vf = MediaHub.volumeFraction();
        volBar.setScaleX(Math.max(0.02f, vf));

        // Conduite
        date.setText(Ui.capitalize(new SimpleDateFormat("EEEE d MMMM", Locale.FRANCE).format(new Date())));
        float sp = VehicleState.speed();
        speed.setText(String.valueOf(sp < 0 ? 0 : Math.round(sp)));
        speed.setTextColor(sp < 0 ? Ui.TEXT3 : Ui.TEXT);
        float amb = VehicleState.ambient;
        ext.setText(Float.isNaN(amb) ? "—" : Ui.dec1(amb) + "°");
        float bv = VehicleState.battery;
        batt.setText(Float.isNaN(bv) ? "—" : Ui.dec1(bv) + " V");
    }
}
