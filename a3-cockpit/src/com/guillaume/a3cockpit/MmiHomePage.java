package com.guillaume.a3cockpit;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Accueil façon Audi MMI : la voiture sur une lueur bleue, le menu disposé sur une orbite
 * elliptique, les anneaux, une grande horloge et le morceau en cours.
 */
final class MmiHomePage extends Page {
    private final float ox, oy, rx, ry;
    final CarSideView car;
    private final TextView clock, date, mediaTitle, mediaSub, tab;
    private final IconView mediaIcon, playIcon;
    private final GradientDrawable dot;
    private final SimpleDateFormat hm = new SimpleDateFormat("HH:mm", Locale.FRANCE);
    private final SimpleDateFormat dm = new SimpleDateFormat("EEEE d MMMM", Locale.FRANCE);

    /** Disposition : "orbit" = carrousel au sol autour de la voiture, "side" = raccourcis dans la barre latérale. */
    static boolean orbit() { return !"side".equals(Prefs.str("home_layout", "orbit")); }

    private final boolean withOrbit;

    MmiHomePage(final MainActivity a, float w, float h) {
        super(a, w, h);
        withOrbit = orbit();
        if (withOrbit) {
            // anneau posé au sol, vu en perspective, la voiture se tient dessus
            ox = w * 0.385f;
            oy = h * 0.60f;
            rx = w * 0.320f;
            ry = h * 0.27f;
        } else {
            ox = w * 0.385f;
            oy = h * 0.62f;
            rx = w * 0.36f;
            ry = h * 0.25f;
        }

        Ui.place(this, new Floor(), 0, 0, w, h);

        float cw, ch, gx, gy;
        if (withOrbit) {
            cw = rx * 1.30f;
            gx = ox - rx * 0.28f;
            gy = oy + ry * 0.04f;
        } else {
            cw = rx * 1.80f;                                    // voiture en grand, centrée
            gx = ox;
            gy = oy + ry * 0.10f;
        }
        ch = cw * 0.80f;
        // dans CarSideView, la lueur (appui des roues) est à 74 % de la hauteur de la vue
        car = Ui.place(this, new CarSideView(a), gx - cw / 2f, gy - ch * 0.74f, cw, ch);
        Ui.onTap(car, new Runnable() { @Override public void run() { car.shine(); } });

        if (withOrbit) {
            // 7 emplacements personnalisables (appui long pour modifier), plus petits au fond
            float[] angles = {-55, -25, 3, 30, 58, 92, 130};
            for (int i = 0; i < OrbitSlots.COUNT; i++) item(angles[i], i);
        }

        // colonne de droite : anneaux, horloge, date, multimédia
        float x0 = withOrbit ? w * 0.815f : w * 0.795f, cwid = w - x0 - 4;
        Ui.place(this, new MmiChrome.Rings(a), x0, 28, cwid, 58);
        TextView brand = Ui.place(this, Ui.text(a, "Audi", 26, Ui.AUDI_RED, Ui.MED), x0, 92, cwid, 32);
        brand.setGravity(Gravity.CENTER);
        clock = Ui.place(this, Ui.text(a, "", 62, Ui.TEXT, Ui.LIGHT), x0, 150, cwid, 74);
        clock.setGravity(Gravity.CENTER);
        date = Ui.place(this, Ui.text(a, "", 15, Ui.TEXT2, Ui.REG), x0, 228, cwid, 19);
        date.setGravity(Gravity.CENTER);
        View sep = Ui.place(this, new View(a), x0 + 20, 268, cwid - 40, 1);
        sep.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0x00FFFFFF, 0x50FFFFFF, 0x00FFFFFF}));

        mediaIcon = Ui.place(this, new IconView(a, IconView.RADIO, 0xFFD5D8DD), x0 + 10, 292, 26, 26);
        mediaTitle = Ui.place(this, Ui.text(a, "", 16, Ui.TEXT, Ui.MED), x0 + 46, 290, cwid - 52, 20);
        mediaSub = Ui.place(this, Ui.text(a, "", 12, Ui.TEXT2, Ui.REG), x0 + 46, 313, cwid - 52, 15);
        View hit = Ui.place(this, new View(a), x0, 282, cwid, 52);
        Ui.onTap(hit, new Runnable() { @Override public void run() { a.openMediaApp(); } });

        float mid = x0 + cwid / 2f;
        ctrl(mid - 74, 350, IconView.PREV, false, new Runnable() { @Override public void run() { MediaHub.prev(); } });
        playIcon = ctrl(mid - 26, 344, IconView.PLAY, true, new Runnable() {
            @Override public void run() { MediaHub.playPause(); a.refreshSoon(); }
        });
        ctrl(mid + 30, 350, IconView.NEXT, false, new Runnable() { @Override public void run() { MediaHub.next(); } });

        // onglet du bas : état du véhicule
        View dv = new View(a);
        dot = Ui.round(Ui.GRAY, 5);
        dv.setBackground(dot);
        Ui.place(this, dv, ox - 150, h - 27, 9, 9);
        tab = Ui.place(this, Ui.text(a, "", 13, Ui.TEXT2, Ui.REG), ox - 134, h - 31, 300, 17);
        Ui.onTap(tab, new Runnable() { @Override public void run() { a.showVehicle(0); } });

        reloadCar();
    }

    void reloadCar() {
        car.set(Prefs.carColor(), Prefs.fiveDoors(), a.carPhoto());
    }

    private void item(float deg, final int slot) {
        final String v = OrbitSlots.get(slot);
        double t = Math.toRadians(deg);
        float x = ox + rx * (float) Math.cos(t), y = oy + ry * (float) Math.sin(t);
        float depth = 0.80f + 0.20f * (float) ((Math.sin(t) + 1) / 2);   // perspective : fond plus petit
        float d = 44 * depth;
        FrameLayout b = Ui.place(this, new FrameLayout(a), x - d / 2f - 4, y - d / 2f - 4, 150, d + 8);
        if (OrbitSlots.empty(a, v)) {                       // emplacement libre : petit « + » discret
            Ui.place(b, new MmiChrome.Orb(a, IconView.PLUS, null, true), 4 + d * 0.22f, 4 + d * 0.22f, d * 0.56f, d * 0.56f);
        } else {
            Ui.place(b, new MmiChrome.Orb(a, OrbitSlots.icon(v), OrbitSlots.appIcon(a, v), false), 4, 4, d, d);
            Ui.place(b, Ui.text(a, OrbitSlots.label(a, v), 14 * (0.9f + 0.1f * depth), 0xFFE4E6EA, Ui.REG),
                    d + 14, d / 2f - 5, 110, 18);
        }
        Ui.onTap(b, new Runnable() {
            @Override public void run() {
                if (OrbitSlots.empty(a, v)) editSlot(slot);
                else OrbitSlots.run(a, v);
            }
        });
        b.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View view) { editSlot(slot); return true; }
        });
    }

    private void editSlot(int slot) {
        OrbitSlots.edit(a, slot, new Runnable() { @Override public void run() { a.rebuild(); } });
    }

    private IconView ctrl(float x, float y, int icon, boolean big, Runnable r) {
        float s = big ? 52 : 40;
        FrameLayout b = Ui.place(this, new FrameLayout(a), x, y, s, s);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(big ? 0x26FFFFFF : 0x00000000);
        g.setStroke(Math.max(1, Ui.s(1)), 0x40FFFFFF);
        b.setBackground(g);
        IconView iv = Ui.place(b, new IconView(a, icon, Ui.TEXT), s * 0.28f, s * 0.28f, s * 0.44f, s * 0.44f);
        Ui.onTap(b, r);
        return iv;
    }

    @Override void refresh() {
        Date now = new Date();
        clock.setText(hm.format(now));
        date.setText(Ui.capitalize(dm.format(now)));

        if (MediaHub.ctl != null) {
            mediaTitle.setText(MediaHub.title != null ? MediaHub.title : (MediaHub.appLabel != null ? MediaHub.appLabel : "Lecture"));
            mediaSub.setText(MediaHub.artist != null ? MediaHub.artist : (MediaHub.appLabel != null ? MediaHub.appLabel : ""));
        } else {
            mediaTitle.setText("Radio");
            mediaSub.setText(MediaHub.access ? "Rien en lecture" : "Touchez pour ouvrir");
        }
        mediaIcon.set(MediaHub.pkg != null && !MediaHub.pkg.contains("radio") ? IconView.MUSIC : IconView.RADIO, 0xFFD5D8DD);
        playIcon.set(MediaHub.playing ? IconView.PAUSE : IconView.PLAY, Ui.TEXT);

        boolean five = Prefs.fiveDoors();
        StringBuilder sb = new StringBuilder(VehicleState.statusText(five));
        float amb = VehicleState.ambient;
        if (!Float.isNaN(amb)) sb.append("   ·   ").append(Ui.dec1(amb)).append(" °C");
        float sp = VehicleState.speed();
        sb.append("   ·   ").append(sp < 0 ? 0 : Math.round(sp)).append(" km/h");
        tab.setText(sb.toString());
        dot.setColor(VehicleState.statusColor(five));
    }

    /** Sol en perspective, anneau argenté (plus brillant devant) et onglet trapèze du bas. */
    private final class Floor extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private final Path tabPath = new Path();

        Floor() { super(MmiHomePage.this.getContext()); }

        @Override protected void onDraw(Canvas c) {
            float k = Ui.scale;
            float cx = ox * k, cy = oy * k, ax = rx * k, ay = ry * k;
            // sol : halo très doux, sans bord (se fond dans le noir)
            p.setStyle(Paint.Style.FILL);
            p.setShader(new RadialGradient(0, 0, 1, new int[]{0x40FFFFFF, 0x14FFFFFF, 0x00FFFFFF},
                    new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            c.save();
            c.translate(cx, cy);
            c.scale(ax * 1.15f, ay * 1.15f);
            c.drawCircle(0, 0, 1, p);
            c.restore();
            p.setShader(null);
            if (withOrbit) {
                // anneau extérieur : discret au fond, brillant devant
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Math.max(2f, 2.6f * k));
                r.set(cx - ax, cy - ay, cx + ax, cy + ay);
                p.setShader(new LinearGradient(0, cy - ay, 0, cy + ay,
                        new int[]{0x18FFFFFF, 0x50FFFFFF, 0xE6FFFFFF}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
                c.drawOval(r, p);
                // anneau intérieur et reflet
                p.setStrokeWidth(Math.max(1f, 1.2f * k));
                r.set(cx - ax * 0.80f, cy - ay * 0.80f, cx + ax * 0.80f, cy + ay * 0.80f);
                p.setShader(new LinearGradient(0, cy - ay, 0, cy + ay, 0x08FFFFFF, 0x40FFFFFF, Shader.TileMode.CLAMP));
                c.drawOval(r, p);
                p.setShader(null);
            }

            // onglet trapèze du bas
            float tw = 360 * k, th = 34 * k, by = getHeight() - 2 * k;
            tabPath.reset();
            tabPath.moveTo(cx - tw / 2f - 22 * k, by - th);
            tabPath.lineTo(cx + tw / 2f + 22 * k, by - th);
            tabPath.lineTo(cx + tw / 2f, by);
            tabPath.lineTo(cx - tw / 2f, by);
            tabPath.close();
            p.setStyle(Paint.Style.FILL);
            p.setShader(new LinearGradient(0, by - th, 0, by, 0xFF1C1E21, 0xFF0B0C0E, Shader.TileMode.CLAMP));
            c.drawPath(tabPath, p);
            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(1f, k));
            p.setColor(0x55FFFFFF);
            c.drawPath(tabPath, p);
            p.setStyle(Paint.Style.FILL);
        }
    }
}
