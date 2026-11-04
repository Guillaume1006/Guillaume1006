package com.guillaume.a3cockpit;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;
import android.widget.FrameLayout;

/** Menu principal en grille façon Audi MMI : tuiles sombres, objet métallique, liseré gris foncé, socle à la couleur du thème. */
final class MenuPage extends Page {
    MenuPage(final MainActivity a, float w, float h) {
        super(a, w, h);
        String[] labels = {"Navigation", "Musique", "Radio", "Téléphone", "Véhicule", "Entretien", "Applications", "Réglages"};
        int[] icons = {IconView.NAV, IconView.MUSIC, IconView.RADIO, IconView.PHONE, IconView.CAR, IconView.WRENCH, IconView.GRID, IconView.GEAR};
        Runnable[] acts = {
                new Runnable() { @Override public void run() { Apps.open(a, Apps.NAV); } },
                new Runnable() { @Override public void run() { Apps.open(a, Apps.MUSIC); } },
                new Runnable() { @Override public void run() { Apps.open(a, Apps.RADIO); } },
                new Runnable() { @Override public void run() { Apps.open(a, Apps.PHONE); } },
                new Runnable() { @Override public void run() { a.showVehicle(0); } },
                new Runnable() { @Override public void run() { a.showVehicle(3); } },
                new Runnable() { @Override public void run() { a.showDrawer(); } },
                new Runnable() { @Override public void run() { a.showSettings(); } },
        };
        float pad = 10, gap = 12;
        float tw = (w - 2 * pad - 3 * gap) / 4f, th = (h - 40 - gap) / 2f;
        for (int i = 0; i < 8; i++) {
            float x = pad + (i % 4) * (tw + gap), y = 8 + (i / 4) * (th + gap);
            FrameLayout t = Ui.place(this, new FrameLayout(a), x, y, tw, th);
            t.setBackground(Ui.gradient(0xFF1C1E21, 0xFF0A0B0D, 6));
            Ui.place(t, new Emblem(icons[i]), (tw - 120) / 2f, th * 0.12f, 120, th * 0.58f);
            Ui.place(t, new IconView(a, icons[i], 0xFFD5D8DD), 16, th - 38, 18, 18);
            Ui.place(t, Ui.text(a, labels[i], 15, Ui.TEXT, Ui.REG), 42, th - 38, tw - 50, 19);
            View bar = Ui.place(t, new View(a), 0, th - 4, tw, 4);
            bar.setBackground(new android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0xFF26282C, 0xFF34373C, 0xFF26282C}));
            Ui.onTap(t, acts[i]);
        }
        // indicateur de page
        View d1 = Ui.place(this, new View(a), w / 2f - 14, h - 18, 16, 4);
        d1.setBackgroundColor(0xFFFFFFFF);
        View d2 = Ui.place(this, new View(a), w / 2f + 6, h - 18, 8, 4);
        d2.setBackgroundColor(0x55FFFFFF);
    }

    /** Grand objet métallique posé sur un socle elliptique. */
    private final class Emblem extends View {
        private final int icon;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Emblem(int icon) {
            super(MenuPage.this.getContext());
            this.icon = icon;
        }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            float cx = w / 2f, cy = h * 0.46f, s = Math.min(w, h) * 0.62f;
            // socle
            p.setShader(new RadialGradient(cx, h * 0.86f, w * 0.48f, new int[]{Ui.alpha(Ui.blend(Ui.ACCENT, 0xFF000000, 0.25f), 0x90), Ui.alpha(Ui.ACCENT, 0)}, null, Shader.TileMode.CLAMP));
            c.save();
            c.scale(1f, 0.22f, cx, h * 0.86f);
            c.drawCircle(cx, h * 0.86f, w * 0.48f, p);
            c.restore();
            p.setShader(null);
            // ombre portée puis icône « chromée » (deux passes décalées)
            IconView.draw(c, icon, 0x99000000, cx + s * 0.04f, cy + s * 0.05f, s);
            IconView.draw(c, icon, 0xFF6E747C, cx, cy + s * 0.02f, s);
            IconView.draw(c, icon, 0xFFE9ECF0, cx - s * 0.015f, cy, s);
        }
    }
}
