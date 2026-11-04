package com.guillaume.a3cockpit;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/** Voiture vue de dessus : ouvrants qui s'allument et s'ouvrent, ou zones de radars. */
final class CarTopView extends View {
    static final int MODE_OPENINGS = 0, MODE_RADAR = 1;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path body = new Path(), tmp = new Path();
    private final RectF r = new RectF();
    private int mode;
    private final float[] doorAnim = new float[6];

    CarTopView(Context c, int mode) {
        super(c);
        this.mode = mode;
        setLayerType(LAYER_TYPE_SOFTWARE, null);   // pour le flou des halos
        glow.setMaskFilter(new BlurMaskFilter(18, BlurMaskFilter.Blur.NORMAL));
        r.set(30, 20, 390, 880);
        body.addRoundRect(r, new float[]{110, 120, 110, 120, 80, 70, 80, 70}, Path.Direction.CW);
    }

    void setMode(int m) { mode = m; invalidate(); }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        boolean radar = mode == MODE_RADAR;
        float bx0 = radar ? -20 : -110, bx1 = radar ? 440 : 530;
        float by0 = radar ? -170 : -20, by1 = radar ? 1070 : 1000;
        float k = Math.min(w / (bx1 - bx0), h / (by1 - by0));
        c.save();
        c.translate(w / 2f - (bx0 + bx1) / 2f * k, h / 2f - (by0 + by1) / 2f * k);
        c.scale(k, k);

        boolean five = Prefs.fiveDoors();
        boolean animating = false;
        for (int i = 0; i < 6; i++) {
            float target = VehicleState.openings[i] == VehicleState.OPEN ? 1f : 0f;
            if (Math.abs(doorAnim[i] - target) > 0.01f) {
                doorAnim[i] += (target - doorAnim[i]) * 0.18f;
                animating = true;
            } else doorAnim[i] = target;
        }

        if (radar) drawRadar(c);
        drawCar(c, five);
        c.restore();
        if (animating) postInvalidateOnAnimation();
    }

    private void drawCar(Canvas c, boolean five) {
        int color = Prefs.carColor();
        int light = Ui.blend(color, 0xFFFFFFFF, 0.3f);
        int dark = Ui.blend(color, 0xFF000000, 0.45f);
        int glass = 0xFF151A22;

        // ombre
        glow.setColor(0x99000000);
        r.set(40, 40, 380, 890);
        c.drawRoundRect(r, 100, 100, glow);

        // roues
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF0C0C0E);
        r.set(16, 150, 50, 270); c.drawRoundRect(r, 10, 10, p);
        r.set(370, 150, 404, 270); c.drawRoundRect(r, 10, 10, p);
        r.set(16, 640, 50, 760); c.drawRoundRect(r, 10, 10, p);
        r.set(370, 640, 404, 760); c.drawRoundRect(r, 10, 10, p);

        // portes ouvertes (dessinées sous la caisse)
        if (five) {
            drawDoorLeaf(c, VehicleState.FL, 30, 300, 200, true, color);
            drawDoorLeaf(c, VehicleState.FR, 390, 300, 200, false, color);
            drawDoorLeaf(c, VehicleState.RL, 30, 500, 175, true, color);
            drawDoorLeaf(c, VehicleState.RR, 390, 500, 175, false, color);
        } else {
            drawDoorLeaf(c, VehicleState.FL, 30, 300, 270, true, color);
            drawDoorLeaf(c, VehicleState.FR, 390, 300, 270, false, color);
        }

        // caisse
        p.setShader(new LinearGradient(30, 0, 390, 0, new int[]{dark, light, color, light, dark},
                new float[]{0f, 0.22f, 0.5f, 0.78f, 1f}, Shader.TileMode.CLAMP));
        c.drawPath(body, p);
        p.setShader(null);

        // capot (avec nervures)
        float hood = doorAnim[VehicleState.HOOD];
        if (hood > 0.01f) {
            highlight(c, 34, 24, 386, 292, 100, hood);
        }
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3);
        p.setColor(0x33000000);
        c.drawLine(130, 60, 120, 290, p);
        c.drawLine(290, 60, 300, 290, p);
        p.setColor(0x55000000);
        tmp.reset();
        tmp.moveTo(44, 292);
        tmp.quadTo(210, 270, 376, 292);
        c.drawPath(tmp, p);
        p.setStyle(Paint.Style.FILL);

        // phares / feux
        p.setColor(0xFFEAF2FF);
        tmp.reset(); tmp.moveTo(58, 92); tmp.lineTo(120, 44); tmp.lineTo(132, 58); tmp.lineTo(70, 112); tmp.close(); c.drawPath(tmp, p);
        tmp.reset(); tmp.moveTo(362, 92); tmp.lineTo(300, 44); tmp.lineTo(288, 58); tmp.lineTo(350, 112); tmp.close(); c.drawPath(tmp, p);
        p.setColor(0xFFE0182D);
        r.set(46, 846, 130, 868); c.drawRoundRect(r, 8, 8, p);
        r.set(290, 846, 374, 868); c.drawRoundRect(r, 8, 8, p);

        // pare-brise, toit, lunette
        p.setShader(new LinearGradient(0, 300, 0, 410, new int[]{0xFF3A4352, glass}, null, Shader.TileMode.CLAMP));
        tmp.reset(); tmp.moveTo(70, 300); tmp.quadTo(210, 284, 350, 300); tmp.lineTo(322, 410); tmp.lineTo(98, 410); tmp.close();
        c.drawPath(tmp, p);
        p.setShader(null);
        p.setColor(Ui.blend(color, 0xFF000000, 0.2f));
        r.set(98, 410, 322, 650); c.drawRoundRect(r, 18, 18, p);
        p.setColor(0x22FFFFFF);
        r.set(120, 430, 300, 470); c.drawRoundRect(r, 12, 12, p);
        p.setShader(new LinearGradient(0, 650, 0, 750, new int[]{glass, 0xFF333B48}, null, Shader.TileMode.CLAMP));
        tmp.reset(); tmp.moveTo(98, 650); tmp.lineTo(322, 650); tmp.lineTo(338, 750); tmp.quadTo(210, 762, 82, 750); tmp.close();
        c.drawPath(tmp, p);
        p.setShader(null);
        // vitres latérales
        p.setColor(glass);
        tmp.reset(); tmp.moveTo(60, 320); tmp.lineTo(92, 412); tmp.lineTo(92, 648); tmp.lineTo(76, 740); tmp.lineTo(58, 730); tmp.close(); c.drawPath(tmp, p);
        tmp.reset(); tmp.moveTo(360, 320); tmp.lineTo(328, 412); tmp.lineTo(328, 648); tmp.lineTo(344, 740); tmp.lineTo(362, 730); tmp.close(); c.drawPath(tmp, p);

        // hayon
        float trunk = doorAnim[VehicleState.TRUNK];
        if (trunk > 0.01f) {
            highlight(c, 40, 756, 380, 878, 70, trunk);
            p.setColor(Ui.alpha(Ui.RED, (int) (200 * trunk)));
            r.set(60, 880, 360, 880 + 70 * trunk);
            c.drawRoundRect(r, 18, 18, p);
        }
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3);
        p.setColor(0x55000000);
        tmp.reset(); tmp.moveTo(48, 758); tmp.quadTo(210, 772, 372, 758); c.drawPath(tmp, p);

        // découpes de portes
        p.setColor(0x66000000);
        c.drawLine(32, 300, 64, 300, p);
        c.drawLine(356, 300, 388, 300, p);
        if (five) {
            c.drawLine(30, 500, 94, 500, p);
            c.drawLine(326, 500, 390, 500, p);
            c.drawLine(32, 676, 80, 676, p);
            c.drawLine(340, 676, 388, 676, p);
        } else {
            c.drawLine(30, 570, 94, 570, p);
            c.drawLine(326, 570, 390, 570, p);
        }
        p.setStyle(Paint.Style.FILL);

        // rétroviseurs
        p.setColor(dark);
        r.set(2, 312, 40, 342); c.drawRoundRect(r, 12, 12, p);
        r.set(380, 312, 418, 342); c.drawRoundRect(r, 12, 12, p);
    }

    private void highlight(Canvas c, float l, float t, float rr, float b, float rad, float a) {
        glow.setColor(Ui.alpha(Ui.RED, (int) (170 * a)));
        r.set(l, t, rr, b);
        c.drawRoundRect(r, rad, rad, glow);
        p.setColor(Ui.alpha(Ui.RED, (int) (120 * a)));
        c.drawRoundRect(r, rad, rad, p);
    }

    private void drawDoorLeaf(Canvas c, int idx, float hx, float hy, float len, boolean left, int color) {
        float a = doorAnim[idx];
        if (a <= 0.01f) return;
        c.save();
        c.rotate((left ? 1 : -1) * 42f * a, hx, hy);
        glow.setColor(Ui.alpha(Ui.RED, (int) (200 * a)));
        float x0 = left ? hx - 4 : hx - 18, x1 = left ? hx + 18 : hx + 4;
        r.set(x0 - 6, hy, x1 + 6, hy + len);
        c.drawRoundRect(r, 14, 14, glow);
        p.setColor(Ui.blend(color, Ui.RED, 0.75f));
        r.set(x0, hy + 4, x1, hy + len);
        c.drawRoundRect(r, 9, 9, p);
        c.restore();
    }

    private void drawRadar(Canvas c) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.BUTT);
        drawSensors(c, 210, 80, 200, VehicleState.radarFront);
        drawSensors(c, 210, 820, 20, VehicleState.radarRear);
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
