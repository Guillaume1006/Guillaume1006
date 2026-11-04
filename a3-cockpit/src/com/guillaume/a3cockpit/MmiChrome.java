package com.guillaume.a3cockpit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Habillage façon Audi MMI : barre du haut, barre latérale, icônes métalliques, anneaux. */
final class MmiChrome {
    private MmiChrome() {}

    /** Barre d'état du haut : maison, titre, température, Bluetooth, heure, retour. */
    static final class Bar extends FrameLayout {
        private final TextView time, temp, title;
        private final IconView bt;
        private final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm", Locale.FRANCE);

        Bar(final MainActivity a, float w) {
            super(a);
            setBackground(Ui.gradient(0xFF1D1F22, 0xFF0C0D0F, 0));
            View line = Ui.place(this, new View(a), 0, 39, w, 1);
            line.setBackground(new android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0x10FFFFFF, 0x55FFFFFF, 0x10FFFFFF}));
            FrameLayout home = Ui.place(this, new FrameLayout(a), 14, 4, 44, 32);
            Ui.place(home, new IconView(a, IconView.HOME, Ui.TEXT), 11, 5, 22, 22);
            Ui.onTap(home, new Runnable() { @Override public void run() { a.showHome(); } });
            title = Ui.place(this, Ui.text(a, "", 14, Ui.TEXT2, Ui.REG), 70, 12, 300, 18);
            temp = Ui.place(this, Ui.text(a, "", 14, Ui.TEXT2, Ui.REG), w - 250, 12, 70, 18);
            temp.setGravity(Gravity.END);
            bt = Ui.place(this, new IconView(a, IconView.BT, Ui.TEXT3), w - 168, 11, 18, 18);
            time = Ui.place(this, Ui.text(a, "", 16, Ui.TEXT, Ui.MED), w - 140, 11, 70, 20);
            time.setGravity(Gravity.CENTER);
            FrameLayout back = Ui.place(this, new FrameLayout(a), w - 58, 4, 44, 32);
            Ui.place(back, new IconView(a, IconView.BACK, Ui.TEXT), 11, 5, 22, 22);
            Ui.onTap(back, new Runnable() { @Override public void run() { a.onBackPressed(); } });
        }

        void setTitle(String t) { title.setText(t); }

        void refresh() {
            time.setText(fmt.format(new Date()));
            float amb = VehicleState.ambient;
            temp.setText(Float.isNaN(amb) ? "" : Ui.dec1(amb) + " °C");
            bt.set(IconView.BT, VehicleState.obdConnected ? Ui.MMI_BLUE : Ui.TEXT3);
        }
    }

    /** Barre latérale gauche : icônes sobres séparées par des filets. */
    static final class Side extends FrameLayout {
        Side(final MainActivity a, float h) {
            super(a);
            setBackground(Ui.gradient(0xFF17181B, 0xFF0B0C0E, 4));
            int[] icons = {IconView.MUSIC, IconView.NAV, IconView.PHONE, IconView.GEAR, IconView.CAR, IconView.GRID};
            Runnable[] acts = {
                    new Runnable() { @Override public void run() { Apps.open(a, Apps.MUSIC); } },
                    new Runnable() { @Override public void run() { Apps.open(a, Apps.NAV); } },
                    new Runnable() { @Override public void run() { Apps.open(a, Apps.PHONE); } },
                    new Runnable() { @Override public void run() { a.showSettings(); } },
                    new Runnable() { @Override public void run() { a.showVehicle(-1); } },
                    new Runnable() { @Override public void run() { a.showApps(); } },
            };
            float cell = Math.min(64, h / icons.length);
            for (int i = 0; i < icons.length; i++) {
                FrameLayout b = Ui.place(this, new FrameLayout(a), 0, i * cell, 66, cell);
                Ui.place(b, new IconView(a, icons[i], 0xFFD5D8DD), 19, (cell - 28) / 2f, 28, 28);
                Ui.onTap(b, acts[i]);
                if (i > 0) {
                    View sep = Ui.place(this, new View(a), 10, i * cell, 40, 1);
                    sep.setBackgroundColor(0x22FFFFFF);
                }
            }
        }
    }

    /** Barre latérale « raccourcis » : les 7 emplacements, chacun posé sur une lueur rouge. */
    static final class SlotSide extends FrameLayout {
        SlotSide(final MainActivity a, float h) {
            super(a);
            setBackground(Ui.gradient(0xFF17181B, 0xFF0B0C0E, 4));
            int n = OrbitSlots.SIDE_COUNT;
            float cell = h / (n + 1);                       // 5 applis + bouton Menu fixe
            for (int i = 0; i < n; i++) {
                final int slot = i;
                final String v = OrbitSlots.get(i);
                boolean empty = OrbitSlots.empty(a, v);
                FrameLayout b = Ui.place(this, new FrameLayout(a), 0, i * cell, 84, cell);
                if (empty) {
                    Ui.place(b, new IconView(a, IconView.PLUS, 0x55FFFFFF), 30, cell * 0.30f, 24, 24);
                } else {
                    android.graphics.drawable.Drawable ic = OrbitSlots.appIcon(a, v);
                    if (ic != null) {
                        android.widget.ImageView iv = Ui.place(b, new android.widget.ImageView(a), 25, cell * 0.12f, 34, 34);
                        iv.setImageDrawable(Ui.grey(ic));
                    } else {
                        Ui.place(b, new IconView(a, OrbitSlots.icon(v), 0xFFC4C8CE), 26, cell * 0.13f, 32, 32);
                    }
                    TextView t = Ui.place(b, Ui.text(a, OrbitSlots.label(a, v), 12, 0xFFC4C8CE, Ui.REG), 2, cell * 0.13f + 38, 80, 15);
                    t.setGravity(Gravity.CENTER);
                }
                Ui.onTap(b, new Runnable() {
                    @Override public void run() {
                        if (OrbitSlots.empty(a, v)) edit(a, slot);
                        else OrbitSlots.run(a, v);
                    }
                });
                b.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override public boolean onLongClick(View view) { edit(a, slot); return true; }
                });
                if (i > 0) {
                    View sep = Ui.place(this, new View(a), 16, i * cell, 52, 1);
                    sep.setBackgroundColor(0x1CFFFFFF);
                }
            }
            // bouton Menu toujours présent en bas
            View sep = Ui.place(this, new View(a), 16, n * cell, 52, 1);
            sep.setBackgroundColor(0x1CFFFFFF);
            FrameLayout m = Ui.place(this, new FrameLayout(a), 0, n * cell, 84, cell);
            Ui.place(m, new IconView(a, IconView.GRID, 0xFFC4C8CE), 26, cell * 0.13f, 32, 32);
            TextView mt = Ui.place(m, Ui.text(a, "Menu", 12, 0xFFC4C8CE, Ui.REG), 2, cell * 0.13f + 38, 80, 15);
            mt.setGravity(Gravity.CENTER);
            Ui.onTap(m, new Runnable() { @Override public void run() { a.showApps(); } });
        }

        private static void edit(final MainActivity a, int slot) {
            OrbitSlots.edit(a, slot, new Runnable() { @Override public void run() { a.rebuild(); } });
        }
    }

    /** Bande lumineuse verticale de la couleur du thème (sépare la barre des applis du reste). */
    static final class Band extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Band(Context c) { super(c); }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), cx = w / 2f;
            int ac = Ui.ACCENT;
            // halo doux
            p.setShader(new LinearGradient(0, 0, w, 0, new int[]{Ui.alpha(ac, 0x00), Ui.alpha(ac, 0x55), Ui.alpha(ac, 0x00)},
                    null, Shader.TileMode.CLAMP));
            c.drawRect(0, h * 0.03f, w, h * 0.97f, p);
            // trait central, estompé aux extrémités
            float lw = Math.max(2f, w * 0.22f);
            p.setShader(new LinearGradient(0, 0, 0, h, new int[]{Ui.alpha(ac, 0x00), ac, Ui.accentLight(), ac, Ui.alpha(ac, 0x00)},
                    new float[]{0f, 0.18f, 0.5f, 0.82f, 1f}, Shader.TileMode.CLAMP));
            c.drawRect(cx - lw / 2f, 0, cx + lw / 2f, h, p);
            p.setShader(null);
        }
    }

    /** Lueur rouge douce sous une icône. */
    static final class Glow extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Glow(Context c) { super(c); }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            p.setShader(new RadialGradient(0, 0, 1, new int[]{0x90E0101E, 0x30C8001A, 0x00C8001A},
                    new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
            c.save();
            c.translate(w / 2f, h * 0.62f);
            c.scale(w / 2f, h * 0.30f);
            c.drawCircle(0, 0, 1, p);
            c.restore();
            p.setShader(null);
        }
    }

    /** Icône ronde métallique (style « objet chromé » des menus Audi). */
    static final class Orb extends View {
        private final int icon;
        private final android.graphics.drawable.Drawable app;
        private final boolean ghost;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Orb(Context c, int icon) { this(c, icon, null, false); }

        /** app : icône d'application affichée dans la pastille ; ghost : emplacement libre. */
        Orb(Context c, int icon, android.graphics.drawable.Drawable app, boolean ghost) {
            super(c);
            this.icon = icon;
            this.app = app;
            this.ghost = ghost;
        }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), r = Math.min(w, h) / 2f - 1;
            float cx = w / 2f, cy = h / 2f;
            if (ghost) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Math.max(1f, r * 0.08f));
                p.setColor(0x55FFFFFF);
                c.drawCircle(cx, cy, r, p);
                p.setStyle(Paint.Style.FILL);
                IconView.draw(c, icon, 0x88FFFFFF, cx, cy, r * 1.1f);
                return;
            }
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xB0101113);
            c.drawCircle(cx, cy, r, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(1f, r * 0.05f));
            p.setColor(0x38FFFFFF);
            c.drawCircle(cx, cy, r - p.getStrokeWidth() / 2f, p);
            p.setStyle(Paint.Style.FILL);
            if (app != null) {                              // icône de l'application, en gris
                int s = Math.round(r * 1.15f);
                android.graphics.drawable.Drawable g = Ui.grey(app);
                g.setBounds(Math.round(cx - s / 2f), Math.round(cy - s / 2f), Math.round(cx + s / 2f), Math.round(cy + s / 2f));
                g.draw(c);
            } else {
                IconView.draw(c, icon, 0xFFB4B8BE, cx, cy, r * 1.0f);
            }
        }
    }

    /** Les quatre anneaux chromés. */
    static final class Rings extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Rings(Context c) { super(c); }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            float r = Math.min(h * 0.46f, w / 6.4f);
            float step = r * 1.42f, total = 2 * r + 3 * step;
            float x0 = (w - total) / 2f + r, cy = h / 2f;
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(r * 0.2f);
            p.setShader(new LinearGradient(0, cy - r, 0, cy + r,
                    new int[]{0xFFFFFFFF, 0xFF8E949C, 0xFFE9ECF0, 0xFF6A7078}, new float[]{0f, 0.45f, 0.6f, 1f},
                    Shader.TileMode.CLAMP));
            for (int i = 0; i < 4; i++) c.drawCircle(x0 + i * step, cy, r - p.getStrokeWidth() / 2f, p);
            p.setShader(null);
        }
    }
}
