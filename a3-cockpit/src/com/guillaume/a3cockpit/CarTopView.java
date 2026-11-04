package com.guillaume.a3cockpit;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/**
 * Audi A3 8P Sportback vue de dessus, ombrée pour donner du volume.
 * Ouvrants animés (portes, capot sur le moteur, hayon sur le coffre) ou zones de radars.
 * Repère : avant en haut, largeur 0..420 (axe x = 210), longueur 18..898 (≈ 4,9 mm / unité).
 */
final class CarTopView extends View {
    static final int MODE_OPENINGS = 0, MODE_RADAR = 1;

    private static final float CX = 210;
    private static final float HOOD_BACK = 336, HATCH_TOP = 726;
    private static final float FD0 = 336, FD1 = 528, RD1 = 694;   // portes (Sportback)

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path body = new Path(), tmp = new Path(), wind = new Path(), rear = new Path(), hoodP = new Path();
    private final RectF r = new RectF();
    private int mode;
    private final float[] anim = new float[6];

    /** Demi-contour droit : suite de courbes de Bézier (c1x,c1y,c2x,c2y,x,y) depuis (210,18). */
    private static final float[][] HALF = {
            {268, 16, 326, 22, 356, 42},     // bouclier AV
            {378, 58, 387, 92, 389, 128},    // angle AV
            {394, 170, 397, 230, 394, 292},  // aile AV (le plus large)
            {390, 380, 384, 480, 386, 580},  // portes (léger galbe)
            {390, 650, 397, 700, 395, 760},  // aile AR
            {393, 822, 384, 864, 362, 882},  // angle AR
            {340, 897, 270, 898, 210, 898},  // bouclier AR
    };

    CarTopView(Context c, int mode) {
        super(c);
        this.mode = mode;
        setLayerType(LAYER_TYPE_SOFTWARE, null);   // pour le flou des halos
        glow.setMaskFilter(new BlurMaskFilter(18, BlurMaskFilter.Blur.NORMAL));
        buildPaths();
    }

    void setMode(int m) { mode = m; invalidate(); }

    private static float mx(float x) { return 2 * CX - x; }

    private void buildPaths() {
        body.reset();
        body.moveTo(CX, 18);
        for (float[] s : HALF) body.cubicTo(s[0], s[1], s[2], s[3], s[4], s[5]);
        // côté gauche = symétrique, parcouru à l'envers
        for (int i = HALF.length - 1; i >= 0; i--) {
            float[] s = HALF[i];
            float sx = i == 0 ? CX : HALF[i - 1][4], sy = i == 0 ? 18 : HALF[i - 1][5];
            body.cubicTo(mx(s[2]), s[3], mx(s[0]), s[1], mx(sx), sy);
        }
        body.close();

        wind.reset();                                      // pare-brise
        wind.moveTo(92, 346);
        wind.quadTo(CX, 318, 328, 346);
        wind.lineTo(306, 462);
        wind.quadTo(CX, 452, 114, 462);
        wind.close();

        hoodP.reset();                                     // capot (ligne de fermeture → pare-brise)
        hoodP.moveTo(30, 122);
        hoodP.quadTo(CX, 70, 390, 122);
        hoodP.lineTo(400, HOOD_BACK + 6);
        hoodP.quadTo(CX, HOOD_BACK - 22, 20, HOOD_BACK + 6);
        hoodP.close();

        rear.reset();                                      // lunette AR
        rear.moveTo(112, 736);
        rear.quadTo(CX, 728, 308, 736);
        rear.lineTo(320, 814);
        rear.quadTo(CX, 826, 100, 814);
        rear.close();
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        boolean radar = mode == MODE_RADAR;
        float bx0 = radar ? -20 : -120, bx1 = radar ? 440 : 540;
        float by0 = radar ? -170 : -10, by1 = radar ? 1080 : 1020;
        float k = Math.min(w / (bx1 - bx0), h / (by1 - by0));
        c.save();
        c.translate(w / 2f - (bx0 + bx1) / 2f * k, h / 2f - (by0 + by1) / 2f * k);
        c.scale(k, k);

        boolean five = Prefs.fiveDoors();
        boolean animating = false;
        for (int i = 0; i < 6; i++) {
            float target = VehicleState.openings[i] == VehicleState.OPEN ? 1f : 0f;
            if (Math.abs(anim[i] - target) > 0.01f) {
                anim[i] += (target - anim[i]) * 0.16f;
                animating = true;
            } else anim[i] = target;
        }

        if (radar) drawRadar(c);
        drawCar(c, five);
        c.restore();
        if (animating) postInvalidateOnAnimation();
    }

    // ------------------------------------------------------------------------------------

    private void fill(int color) {
        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
    }

    private void stroke(int color, float width) {
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(width);
        p.setColor(color);
    }

    /** Dessine un chemin et son symétrique. */
    private void both(Canvas c, Path path) {
        c.drawPath(path, p);
        c.save();
        c.scale(-1, 1, CX, 0);
        c.drawPath(path, p);
        c.restore();
    }

    private void drawCar(Canvas c, boolean five) {
        int color = Prefs.carColor();
        int hi = Ui.blend(color, 0xFFFFFFFF, 0.38f);
        int mid = Ui.blend(color, 0xFFFFFFFF, 0.14f);
        int dark = Ui.blend(color, 0xFF000000, 0.42f);
        int darker = Ui.blend(color, 0xFF000000, 0.68f);
        int glass = 0xFF10141B;

        // Ombre portée
        glow.setColor(0xB0000000);
        r.set(34, 40, 386, 912);
        c.drawRoundRect(r, 110, 110, glow);

        // Pneus (dépassent légèrement des ailes)
        fill(0xFF0A0A0C);
        float[][] tires = {{20, 150, 50, 276}, {370, 150, 400, 276}, {20, 660, 50, 786}, {370, 660, 400, 786}};
        for (float[] t : tires) {
            r.set(t[0], t[1], t[2], t[3]);
            c.drawRoundRect(r, 12, 12, p);
        }
        stroke(0xFF2A2B30, 2);
        for (float[] t : tires) {
            for (float y = t[1] + 12; y < t[3] - 6; y += 14) c.drawLine(t[0] + 4, y, t[2] - 4, y, p);
        }

        // Portes ouvertes (sous la caisse)
        if (five) {
            leaf(c, VehicleState.FL, 27, FD0, FD1 - FD0 - 4, true, color, hi, darker);
            leaf(c, VehicleState.FR, 393, FD0, FD1 - FD0 - 4, false, color, hi, darker);
            leaf(c, VehicleState.RL, 30, FD1, RD1 - FD1 - 4, true, color, hi, darker);
            leaf(c, VehicleState.RR, 390, FD1, RD1 - FD1 - 4, false, color, hi, darker);
        } else {
            leaf(c, VehicleState.FL, 27, FD0, 270, true, color, hi, darker);
            leaf(c, VehicleState.FR, 393, FD0, 270, false, color, hi, darker);
        }

        // ---------- Caisse : dégradé transversal pour le volume ----------
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(26, 0, 394, 0,
                new int[]{darker, dark, mid, hi, mid, dark, darker},
                new float[]{0f, 0.07f, 0.24f, 0.5f, 0.76f, 0.93f, 1f}, Shader.TileMode.CLAMP));
        c.drawPath(body, p);
        p.setShader(null);

        c.save();
        c.clipPath(body);
        // reflets longitudinaux sur les épaules et l'axe du capot
        p.setShader(new LinearGradient(56, 0, 96, 0, new int[]{0x00FFFFFF, 0x30FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
        c.drawRect(56, 90, 96, 880, p);
        p.setShader(new LinearGradient(324, 0, 364, 0, new int[]{0x00FFFFFF, 0x26FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
        c.drawRect(324, 90, 364, 880, p);
        p.setShader(new LinearGradient(176, 0, 244, 0, new int[]{0x00FFFFFF, 0x2CFFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
        c.drawRect(176, 80, 244, HOOD_BACK, p);
        // occlusion sur le pourtour (arrondi de la caisse)
        stroke(0x70000000, 14);
        c.drawPath(body, p);
        // ouvertures de portes : habitacle visible
        float[][] apertures = five
                ? new float[][]{{VehicleState.FL, FD0, FD1}, {VehicleState.RL, FD1, RD1}}
                : new float[][]{{VehicleState.FL, FD0, FD0 + 270}};
        for (float[] ap : apertures) {
            int i = (int) ap[0];
            aperture(c, anim[i], ap[1], ap[2], true);
            aperture(c, anim[i + 1], ap[1], ap[2], false);
        }
        c.restore();

        // ---------- Avant ----------
        // calandre single-frame vue du dessus
        tmp.reset();
        tmp.moveTo(128, 26);
        tmp.quadTo(CX, 14, 292, 26);
        tmp.lineTo(282, 44);
        tmp.quadTo(CX, 34, 138, 44);
        tmp.close();
        fill(0xFF0B0C0F);
        c.drawPath(tmp, p);
        stroke(0xFFC9CED6, 2.4f);
        c.drawPath(tmp, p);
        // phares avec feux de jour LED
        tmp.reset();
        tmp.moveTo(296, 44);
        tmp.quadTo(352, 40, 380, 74);
        tmp.lineTo(388, 112);
        tmp.quadTo(344, 94, 300, 66);
        tmp.close();
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(296, 40, 388, 112, new int[]{0xFF2A3038, 0xFFB8C4D4}, null, Shader.TileMode.CLAMP));
        both(c, tmp);
        tmp.reset();
        tmp.moveTo(304, 62);
        tmp.quadTo(346, 84, 384, 102);
        stroke(0x88FFFFFF, 7);
        both(c, tmp);
        stroke(0xFFFFFFFF, 2.6f);
        both(c, tmp);
        // ligne de capot + nervures en V
        stroke(0x66000000, 2.5f);
        tmp.reset();
        tmp.moveTo(46, 116);
        tmp.quadTo(CX, 70, 374, 116);
        c.drawPath(tmp, p);
        tmp.reset();
        tmp.moveTo(150, HOOD_BACK - 8);
        tmp.quadTo(146, 200, 170, 92);
        stroke(0x40000000, 3);
        both(c, tmp);
        c.save();
        c.translate(4, 0);
        stroke(0x30FFFFFF, 2);
        c.drawPath(tmp, p);
        c.restore();

        // capot ouvert
        float hood = anim[VehicleState.HOOD];
        if (hood > 0.01f) drawHoodOpen(c, hood, color, hi, darker);

        // ---------- Vitrage, pavillon ----------
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0, 320, 0, 462, new int[]{0xFF3D4757, glass}, null, Shader.TileMode.CLAMP));
        c.drawPath(wind, p);
        p.setShader(null);
        c.save();
        c.clipPath(wind);
        fill(0x1EFFFFFF);
        tmp.reset();
        tmp.moveTo(150, 320);
        tmp.lineTo(196, 320);
        tmp.lineTo(150, 470);
        tmp.lineTo(104, 470);
        tmp.close();
        c.drawPath(tmp, p);
        c.restore();
        stroke(0xFF050608, 3);
        c.drawPath(wind, p);

        // vitres latérales
        tmp.reset();
        tmp.moveTo(96, 354);
        tmp.lineTo(112, 464);
        tmp.lineTo(112, 724);
        tmp.lineTo(102, 798);
        tmp.lineTo(86, 790);
        tmp.quadTo(78, 600, 84, 400);
        tmp.close();
        fill(glass);
        both(c, tmp);
        // montants (B et custode)
        fill(0xFF06070A);
        if (five) {
            r.set(80, FD1 - 7, 112, FD1 + 7);
            c.drawRect(r, p);
            r.set(308, FD1 - 7, 340, FD1 + 7);
            c.drawRect(r, p);
            r.set(82, RD1 - 6, 112, RD1 + 6);
            c.drawRect(r, p);
            r.set(308, RD1 - 6, 338, RD1 + 6);
            c.drawRect(r, p);
        }

        // pavillon bombé
        r.set(110, 458, 310, 734);
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(110, 0, 310, 0, new int[]{dark, hi, mid, dark}, new float[]{0f, 0.35f, 0.7f, 1f}, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, 34, 34, p);
        p.setShader(new RadialGradient(170, 540, 120, 0x30FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, 34, 34, p);
        p.setShader(null);
        // antenne
        fill(0xFF08090B);
        r.set(203, 690, 217, 716);
        c.drawRoundRect(r, 6, 6, p);

        // ---------- Arrière ----------
        float trunk = anim[VehicleState.TRUNK];
        if (trunk > 0.01f) drawHatchOpen(c, trunk, color, hi, dark, darker, glass);
        else drawHatch(c, glass, darker, 0f);

        // feux AR (débordent sur les ailes)
        tmp.reset();
        tmp.moveTo(298, 884);
        tmp.quadTo(362, 882, 386, 842);
        tmp.lineTo(394, 798);
        tmp.quadTo(372, 852, 298, 868);
        tmp.close();
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(298, 0, 394, 0, new int[]{0xFF6A0008, 0xFFE0121E}, null, Shader.TileMode.CLAMP));
        both(c, tmp);
        tmp.reset();
        tmp.moveTo(306, 876);
        tmp.quadTo(360, 870, 384, 830);
        stroke(0xFFFF8F8F, 2.2f);
        both(c, tmp);

        // découpes de portes
        stroke(0x99000000, 2.2f);
        float[] seams = five ? new float[]{FD0, FD1, RD1} : new float[]{FD0, FD0 + 270};
        for (float y : seams) {
            c.drawLine(28, y, 70, y, p);
            c.drawLine(350, y, 392, y, p);
        }
        // poignées
        fill(0x55FFFFFF);
        r.set(30, FD1 - 60, 36, FD1 - 30);
        c.drawRoundRect(r, 3, 3, p);
        r.set(384, FD1 - 60, 390, FD1 - 30);
        c.drawRoundRect(r, 3, 3, p);

        // rétroviseurs (avec répétiteur)
        tmp.reset();
        tmp.moveTo(66, 340);
        tmp.lineTo(14, 334);
        tmp.quadTo(2, 338, 4, 352);
        tmp.lineTo(10, 366);
        tmp.lineTo(66, 372);
        tmp.close();
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(4, 0, 66, 0, new int[]{dark, hi}, null, Shader.TileMode.CLAMP));
        both(c, tmp);
        tmp.reset();
        tmp.moveTo(8, 344);
        tmp.lineTo(12, 362);
        stroke(0xFFFFA020, 3);
        both(c, tmp);

        // halos rouges des ouvrants ouverts
        if (hood > 0.01f) halo(c, 40, 80 + 100 * hood, 380, HOOD_BACK, 60, hood);
        if (trunk > 0.01f) halo(c, 44, HATCH_TOP, 376, 896, 70, trunk);
    }

    private void drawHatch(Canvas c, int glass, int darker, float dy) {
        // becquet
        fill(darker);
        r.set(104, 714 + dy, 316, 738 + dy);
        c.drawRoundRect(r, 12, 12, p);
        stroke(0xFFFF2A2A, 3);
        c.drawLine(182, 724 + dy, 238, 724 + dy, p);
        // lunette
        c.save();
        c.translate(0, dy);
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0, 736, 0, 820, new int[]{glass, 0xFF323B49}, null, Shader.TileMode.CLAMP));
        c.drawPath(rear, p);
        p.setShader(null);
        stroke(0xFF050608, 3);
        c.drawPath(rear, p);
        // essuie-glace
        stroke(0xFF050608, 2.5f);
        c.drawLine(CX, 812, 160, 772, p);
        // ligne de hayon
        stroke(0x66000000, 2.5f);
        tmp.reset();
        tmp.moveTo(56, 818);
        tmp.quadTo(CX, 834, 364, 818);
        c.drawPath(tmp, p);
        c.restore();
    }

    private void drawHatchOpen(Canvas c, float a, int color, int hi, int dark, int darker, int glass) {
        // coffre visible
        c.save();
        c.clipPath(body);
        c.clipRect(0, HATCH_TOP, 420, 900);
        fill(Ui.blend(0xFF15171B, color, 1f - a));
        c.drawRect(0, HATCH_TOP, 420, 900, p);
        fill(Ui.alpha(0xFF2A2D33, (int) (255 * a)));
        r.set(80, 760, 340, 870);
        c.drawRoundRect(r, 16, 16, p);
        c.restore();
        // hayon relevé : recule et passe au-dessus du bouclier
        float dy = 120 * a;
        c.save();
        c.translate(0, dy);
        r.set(46, HATCH_TOP, 374, 880);
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(46, 0, 374, 0, new int[]{darker, hi, darker}, null, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, 50, 50, p);
        p.setShader(null);
        c.restore();
        drawHatch(c, glass, darker, dy);
        fill(Ui.alpha(Ui.RED, (int) (90 * a)));
        r.set(46, HATCH_TOP + dy, 374, 880 + dy);
        c.drawRoundRect(r, 50, 50, p);
        glow.setColor(Ui.alpha(Ui.RED, (int) (150 * a)));
        c.drawRoundRect(r, 50, 50, glow);
    }

    private void drawHoodOpen(Canvas c, float a, int color, int hi, int darker) {
        // compartiment moteur
        c.save();
        c.clipPath(body);
        c.clipPath(hoodP);
        fill(Ui.alpha(0xFF141619, (int) (255 * a)));
        c.drawRect(0, 60, 420, HOOD_BACK + 10, p);
        fill(Ui.alpha(0xFF3A3E45, (int) (255 * a)));       // cache moteur
        r.set(130, 150, 290, 270);
        c.drawRoundRect(r, 22, 22, p);
        fill(Ui.alpha(0xFF50555E, (int) (255 * a)));
        r.set(150, 170, 270, 196);
        c.drawRoundRect(r, 8, 8, p);
        fill(Ui.alpha(0xFF24272D, (int) (255 * a)));       // batterie, vase d'expansion
        r.set(300, 200, 352, 290);
        c.drawRoundRect(r, 8, 8, p);
        r.set(66, 150, 110, 210);
        c.drawRoundRect(r, 10, 10, p);
        c.restore();

        // capot relevé (charnières côté pare-brise) : vu raccourci, plus clair car plus près
        c.save();
        c.scale(1f + 0.04f * a, 1f - 0.5f * a, CX, HOOD_BACK);
        c.clipPath(body);
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(26, 0, 394, 0,
                new int[]{darker, hi, Ui.blend(hi, 0xFFFFFFFF, 0.25f), hi, darker}, null, Shader.TileMode.CLAMP));
        c.drawPath(hoodP, p);
        p.setShader(null);
        tmp.reset();
        tmp.moveTo(150, HOOD_BACK - 10);
        tmp.quadTo(146, 200, 170, 96);
        stroke(0x50000000, 3);
        both(c, tmp);
        fill(Ui.alpha(Ui.RED, (int) (70 * a)));
        c.drawPath(hoodP, p);
        stroke(Ui.alpha(Ui.RED, (int) (230 * a)), 3);
        c.drawPath(hoodP, p);
        c.restore();
    }

    /** Intérieur visible quand une porte est ouverte. */
    private void aperture(Canvas c, float a, float y0, float y1, boolean left) {
        if (a <= 0.01f) return;
        float x0 = left ? 20 : 340, x1 = left ? 80 : 400;
        fill(Ui.alpha(0xFF141518, (int) (255 * a)));
        r.set(x0, y0 + 4, x1, y1 - 4);
        c.drawRect(r, p);
        fill(Ui.alpha(0xFF2B2E34, (int) (255 * a)));   // assise
        r.set(left ? 46 : 344, y0 + 40, left ? 76 : 374, y1 - 50);
        c.drawRoundRect(r, 10, 10, p);
    }

    private void halo(Canvas c, float l, float t, float rr, float b, float rad, float a) {
        glow.setColor(Ui.alpha(Ui.RED, (int) (130 * a)));
        r.set(l, t, rr, b);
        c.drawRoundRect(r, rad, rad, glow);
    }

    /** Porte ouverte : panneau qui pivote sur sa charnière avant. */
    private void leaf(Canvas c, int idx, float hx, float hy, float len, boolean left, int color, int hi, int darker) {
        float a = anim[idx];
        if (a <= 0.01f) return;
        c.save();
        c.rotate((left ? 1 : -1) * 48f * a, hx, hy);
        float x0 = left ? hx - 18 : hx - 4, x1 = left ? hx + 4 : hx + 18;
        glow.setColor(Ui.alpha(Ui.RED, (int) (190 * a)));
        r.set(x0 - 6, hy, x1 + 6, hy + len);
        c.drawRoundRect(r, 14, 14, glow);
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(x0, 0, x1, 0, left ? new int[]{darker, hi} : new int[]{hi, darker}, null, Shader.TileMode.CLAMP));
        r.set(x0, hy + 3, x1, hy + len);
        c.drawRoundRect(r, 9, 9, p);
        p.setShader(null);
        fill(0xFF0A0B0E);                                    // garniture intérieure
        r.set(left ? x1 - 6 : x0, hy + 14, left ? x1 : x0 + 6, hy + len - 12);
        c.drawRect(r, p);
        fill(Ui.alpha(Ui.RED, (int) (110 * a)));
        r.set(x0, hy + 3, x1, hy + len);
        c.drawRoundRect(r, 9, 9, p);
        c.restore();
    }

    // ------------------------------------------------------------------------------------

    private void drawRadar(Canvas c) {
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.BUTT);
        drawSensors(c, CX, 80, 200, VehicleState.radarFront);
        drawSensors(c, CX, 838, 20, VehicleState.radarRear);
        p.setStyle(Paint.Style.FILL);
    }

    /** 4 secteurs x 3 bandes. start = angle de départ (degrés), balayage total 140°. */
    private void drawSensors(Canvas c, float cx, float cy, float start, int[] d) {
        boolean live = System.currentTimeMillis() - VehicleState.radarTime < 5000;
        float sector = 140f / 4f;
        for (int s = 0; s < 4; s++) {
            int dist = live ? d[start > 180 ? s : 3 - s] : -1;
            int lit = dist < 0 || dist > 150 ? 0 : dist <= 35 ? 3 : dist <= 80 ? 2 : 1;
            int col = VehicleState.radarColor(dist);
            for (int band = 0; band < 3; band++) {
                float rad = 120 + band * 34;
                boolean on = band >= 3 - lit;
                p.setStrokeWidth(24);
                p.setColor(on ? col : 0x1FFFFFFF);
                r.set(cx - rad, cy - rad, cx + rad, cy + rad);
                c.drawArc(r, start + s * sector + 2.5f, sector - 5f, false, p);
            }
        }
    }
}
