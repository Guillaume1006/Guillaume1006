package com.guillaume.a3cockpit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/**
 * Audi A3 8P Sportback phase 2 (2008-2012) vue de profil, côté droit, posée sur un plateau.
 * Échelle : 1 unité ≈ 4,5 mm. Longueur 4 292 mm, hauteur 1 423 mm, empattement 2 578 mm,
 * porte-à-faux AV 893 mm / AR 821 mm, jantes 17" (pneu Ø ≈ 634 mm). Sol à y = 332.
 */
final class CarSideView extends View {
    private static final float RW = 219, FW = 792, WY = 262;   // centres des roues

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Path body = new Path(), dlo = new Path(), tmp = new Path();
    private final RectF r = new RectF();
    private int color = 0xFF55595F;
    private boolean five = true;
    private Bitmap photo;
    private float sweep = -1f;  // reflet animé

    CarSideView(Context c) {
        super(c);
        buildPaths();
    }

    void set(int color, boolean five, Bitmap photo) {
        this.color = color;
        this.five = five;
        this.photo = photo;
        buildPaths();
        invalidate();
    }

    /** Petit reflet lumineux qui balaie la carrosserie. */
    void shine() {
        sweep = 0f;
        postInvalidateOnAnimation();
    }

    private void buildPaths() {
        body.reset();
        body.moveTo(62, 294);                  // bas du bouclier AR
        body.lineTo(42, 268);
        body.quadTo(32, 232, 36, 190);
        body.lineTo(40, 156);                  // haut des feux
        if (five) {
            body.quadTo(44, 108, 70, 52);      // hayon peu incliné (Sportback)
            body.quadTo(80, 34, 108, 30);      // becquet
            body.quadTo(300, 12, 470, 18);     // pavillon long
        } else {
            body.quadTo(50, 104, 92, 58);      // hayon plus fuyant (3 portes)
            body.quadTo(112, 36, 166, 29);
            body.quadTo(330, 10, 470, 19);
        }
        body.quadTo(516, 22, 548, 38);
        body.lineTo(668, 120);                 // pare-brise
        body.quadTo(712, 134, 780, 142);
        body.lineTo(912, 158);                 // capot
        body.quadTo(962, 166, 980, 188);
        body.quadTo(996, 212, 994, 250);       // calandre single-frame
        body.quadTo(992, 286, 962, 296);
        body.lineTo(874, 294);
        r.set(FW - 80, WY - 80, FW + 80, WY + 80);
        body.arcTo(r, 0, -180, false);
        body.lineTo(710, 294);
        body.lineTo(301, 294);
        r.set(RW - 80, WY - 80, RW + 80, WY + 80);
        body.arcTo(r, 0, -180, false);
        body.lineTo(137, 294);
        body.close();

        dlo.reset();
        if (five) {
            dlo.moveTo(126, 128);
            dlo.quadTo(132, 84, 160, 52);
            dlo.quadTo(176, 40, 204, 38);
        } else {
            dlo.moveTo(148, 128);
            dlo.quadTo(158, 86, 184, 56);
            dlo.quadTo(198, 43, 226, 41);
        }
        dlo.quadTo(340, 26, 462, 33);
        dlo.quadTo(504, 36, 532, 50);
        dlo.lineTo(640, 130);
        dlo.close();
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        if (photo != null) {
            drawPhotoScene(c, w, h);
        } else {
            drawVectorScene(c, w, h);
        }
        if (sweep >= 0f) {
            sweep += 0.025f;
            if (sweep > 1.2f) sweep = -1f;
            postInvalidateOnAnimation();
        }
    }

    /** Photo (vue de 3/4) posée sur le plateau, mise à l'échelle sur toute la zone. */
    private void drawPhotoScene(Canvas c, float w, float h) {
        float ar = photo.getWidth() / (float) photo.getHeight();
        float th = h * 0.88f, tw = th * ar;
        if (tw > w * 0.98f) { tw = w * 0.98f; th = tw / ar; }
        float bottom = h * 0.95f, cx = w / 2f;
        float ty = bottom - th * 0.09f;                 // ligne de contact des roues
        float rx = tw * 0.56f, ry = th * 0.15f;

        // plateau
        p.setStyle(Paint.Style.FILL);
        p.setShader(new RadialGradient(cx, ty, rx, new int[]{0xAA000000, 0x66101018, 0x00000000},
                new float[]{0f, 0.7f, 1f}, Shader.TileMode.CLAMP));
        c.save();
        c.scale(1f, ry / rx, cx, ty);
        c.drawCircle(cx, ty, rx, p);
        c.restore();
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1.5f, w / 400f));
        r.set(cx - rx, ty - ry, cx + rx, ty + ry);
        p.setShader(new LinearGradient(cx - rx, 0, cx + rx, 0, new int[]{0x00FFFFFF, 0x70FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
        c.drawOval(r, p);
        r.set(cx - rx * 0.82f, ty - ry * 0.7f, cx + rx * 0.82f, ty + ry * 0.7f);
        p.setShader(new LinearGradient(cx - rx, 0, cx + rx, 0, new int[]{0x00FFFFFF, 0x30FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
        c.drawOval(r, p);
        p.setShader(null);
        p.setStyle(Paint.Style.FILL);

        // ombre sous la voiture
        p.setShader(new RadialGradient(0, 0, 1, 0xDD000000, 0x00000000, Shader.TileMode.CLAMP));
        c.save();
        c.translate(cx, ty - th * 0.03f);
        c.scale(tw * 0.47f, th * 0.1f);
        c.drawCircle(0, 0, 1, p);
        c.restore();
        p.setShader(null);

        r.set(cx - tw / 2f, bottom - th, cx + tw / 2f, bottom);
        c.drawBitmap(photo, null, r, p);

        // reflet animé (touchez la voiture)
        if (sweep >= 0f) {
            c.save();
            c.clipRect(r);
            float x = r.left - tw * 0.3f + sweep * tw * 1.6f;
            p.setShader(new LinearGradient(x - tw * 0.08f, 0, x + tw * 0.08f, 0,
                    new int[]{0x00FFFFFF, 0x40FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
            c.drawRect(r, p);
            p.setShader(null);
            c.restore();
        }
    }

    private void drawVectorScene(Canvas c, float w, float h) {
        float k = Math.min(w / 1100f, h / 400f);
        c.save();
        c.translate(w / 2f - 510 * k, h / 2f - 205 * k);
        c.scale(k, k);
        drawTurntable(c);
        drawCar(c);
        c.restore();
    }

    private void drawTurntable(Canvas c) {
        p.setShader(new RadialGradient(510, 344, 540, new int[]{0x99000000, 0x66101018, 0x00000000},
                new float[]{0f, 0.7f, 1f}, Shader.TileMode.CLAMP));
        p.setStyle(Paint.Style.FILL);
        r.set(-30, 292, 1050, 396);
        c.drawOval(r, p);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2.5f);
        p.setShader(new LinearGradient(-30, 0, 1050, 0, new int[]{0x00FFFFFF, 0x66FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
        c.drawOval(r, p);
        r.set(50, 308, 970, 380);
        p.setShader(new LinearGradient(50, 0, 970, 0, new int[]{0x00FFFFFF, 0x30FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
        c.drawOval(r, p);
        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
    }

    private Path poly(float... xy) {
        tmp.reset();
        tmp.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) tmp.lineTo(xy[i], xy[i + 1]);
        tmp.close();
        return tmp;
    }

    private void stroke(int color, float w) {
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(w);
        p.setColor(color);
    }

    private void fill(int color) {
        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
    }

    private void drawCar(Canvas c) {
        int light = Ui.blend(color, 0xFFFFFFFF, 0.42f);
        int mid = Ui.blend(color, 0xFFFFFFFF, 0.12f);
        int dark = Ui.blend(color, 0xFF000000, 0.45f);
        int darker = Ui.blend(color, 0xFF000000, 0.7f);
        int trim = 0xFF0C0D10;

        // Ombre au sol
        p.setShader(new RadialGradient(0, 0, 1, 0xCC000000, 0x00000000, Shader.TileMode.CLAMP));
        c.save();
        c.translate(512, 334);
        c.scale(510, 24);
        c.drawCircle(0, 0, 1, p);
        c.restore();
        p.setShader(null);

        // Passages de roue
        fill(0xFF07070A);
        r.set(FW - 80, WY - 80, FW + 80, WY + 80);
        c.drawArc(r, 180, 180, true, p);
        r.set(RW - 80, WY - 80, RW + 80, WY + 80);
        c.drawArc(r, 180, 180, true, p);

        // Caisse : dégradé métallisé
        p.setShader(new LinearGradient(0, 16, 0, 298,
                new int[]{light, mid, color, dark, darker}, new float[]{0f, 0.38f, 0.55f, 0.84f, 1f}, Shader.TileMode.CLAMP));
        c.drawPath(body, p);
        p.setShader(null);

        c.save();
        c.clipPath(body);
        // Zone sous la ligne tornado légèrement plus sombre (effet d'arête)
        p.setShader(new LinearGradient(0, 160, 0, 200, new int[]{0x30FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
        c.drawRect(0, 160, 1000, 200, p);
        p.setShader(new LinearGradient(0, 120, 0, 162, new int[]{0x00FFFFFF, 0x38FFFFFF}, null, Shader.TileMode.CLAMP));
        c.drawRect(0, 120, 1000, 162, p);
        p.setShader(null);
        // Bas de caisse noir mat
        fill(0x66000000);
        c.drawRect(0, 270, 1000, 300, p);
        // Reflet animé
        if (sweep >= 0f) {
            float x = -200 + sweep * 1400;
            p.setShader(new LinearGradient(x - 90, 0, x + 90, 0, new int[]{0x00FFFFFF, 0x55FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
            c.drawPath(poly(x - 60, 0, x + 40, 0, x - 20, 300, x - 120, 300), p);
            p.setShader(null);
        }
        c.restore();

        // Ligne tornado (du feu AR au phare) + ligne dynamique basse
        stroke(0x60FFFFFF, 2.4f);
        c.drawLine(42, 158, 900, 167, p);
        stroke(0x50000000, 2.4f);
        c.drawLine(42, 162, 900, 171, p);
        stroke(0x30FFFFFF, 2f);
        c.drawLine(304, 250, 708, 238, p);

        // Vitrage
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0, 32, 0, 130, new int[]{0xFF3A4352, 0xFF141820, 0xFF0B0D12}, null, Shader.TileMode.CLAMP));
        c.drawPath(dlo, p);
        p.setShader(null);
        c.save();
        c.clipPath(dlo);
        fill(0x1CFFFFFF);
        c.drawPath(poly(300, 26, 384, 26, 330, 134, 246, 134), p);
        c.drawPath(poly(520, 26, 546, 26, 492, 134, 466, 134), p);
        c.restore();

        // Montants noirs (pied milieu + séparation custode)
        fill(trim);
        if (five) {
            c.drawRect(394, 30, 410, 130, p);
            c.drawPath(poly(212, 128, 228, 39, 248, 38, 238, 128), p);
        } else {
            c.drawPath(poly(296, 128, 314, 37, 336, 36, 326, 128), p);
        }
        stroke(trim, 3.5f);                         // encadrement de vitres
        c.drawPath(dlo, p);
        stroke(0x55FFFFFF, 1.2f);                   // fin liseré
        c.drawLine(130, 131, 636, 133, p);

        // Lunette arrière vue de biais + becquet
        fill(0xFF12161D);
        tmp.reset();
        if (five) {
            tmp.moveTo(48, 142);
            tmp.quadTo(52, 100, 74, 58);
            tmp.lineTo(100, 46);
            tmp.lineTo(104, 54);
            tmp.quadTo(84, 72, 74, 106);
            tmp.lineTo(64, 144);
        } else {
            tmp.moveTo(56, 132);
            tmp.quadTo(66, 92, 100, 62);
            tmp.lineTo(140, 44);
            tmp.lineTo(146, 54);
            tmp.quadTo(108, 74, 90, 104);
            tmp.lineTo(74, 134);
        }
        tmp.close();
        c.drawPath(tmp, p);
        fill(darker);
        tmp.reset();
        if (five) {
            tmp.moveTo(112, 30);
            tmp.quadTo(84, 33, 68, 52);
            tmp.lineTo(76, 57);
            tmp.quadTo(90, 40, 114, 37);
        } else {
            tmp.moveTo(166, 29);
            tmp.quadTo(120, 34, 92, 58);
            tmp.lineTo(100, 62);
            tmp.quadTo(124, 42, 168, 36);
        }
        tmp.close();
        c.drawPath(tmp, p);

        // Découpes de portes
        stroke(0x99000000, 2f);
        c.drawLine(650, 130, 660, 292, p);
        tmp.reset();
        if (five) {
            c.drawLine(402, 130, 404, 292, p);
            tmp.moveTo(238, 128);
            tmp.quadTo(244, 196, 292, 220);
        } else {
            tmp.moveTo(326, 128);
            tmp.quadTo(336, 200, 330, 276);
        }
        c.drawPath(tmp, p);
        // Trappe à carburant (côté droit)
        stroke(0x80000000, 1.6f);
        r.set(150, 176, 188, 206);
        c.drawRoundRect(r, 8, 8, p);

        // Poignées sous la ligne tornado
        fill(Ui.blend(color, 0xFF000000, 0.35f));
        r.set(568, 176, 606, 184);
        c.drawRoundRect(r, 4, 4, p);
        if (five) {
            r.set(336, 174, 372, 182);
            c.drawRoundRect(r, 4, 4, p);
        }
        fill(0x40FFFFFF);
        r.set(570, 175, 604, 177);
        c.drawRect(r, p);

        // Rétroviseur avec répétiteur intégré (phase 2)
        tmp.reset();
        tmp.moveTo(600, 104);
        tmp.quadTo(626, 92, 650, 100);
        tmp.lineTo(656, 126);
        tmp.lineTo(608, 128);
        tmp.close();
        p.setShader(new LinearGradient(0, 94, 0, 128, new int[]{light, dark}, null, Shader.TileMode.CLAMP));
        c.drawPath(tmp, p);
        p.setShader(null);
        stroke(0xFFFFA020, 2.5f);
        c.drawLine(612, 124, 650, 121, p);

        // Phare (feux de jour LED en vague)
        tmp.reset();
        tmp.moveTo(858, 152);
        tmp.lineTo(956, 164);
        tmp.quadTo(978, 174, 984, 196);
        tmp.quadTo(940, 202, 894, 192);
        tmp.quadTo(872, 176, 858, 152);
        tmp.close();
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(858, 0, 984, 0, new int[]{0xFF2A2F38, 0xFF8C96A4, 0xFFDDE4EE}, null, Shader.TileMode.CLAMP));
        c.drawPath(tmp, p);
        p.setShader(null);
        stroke(0xFF1A1C20, 1.5f);
        c.drawPath(tmp, p);
        fill(0xFF0E1014);
        c.drawCircle(934, 177, 10, p);
        fill(0xFFB8C6DA);
        c.drawCircle(934, 177, 5, p);
        stroke(0x66FFFFFF, 7f);
        drawDrl(c);
        stroke(0xFFFFFFFF, 3f);
        drawDrl(c);

        // Calandre single-frame (chromée) + antibrouillard
        fill(0xFF101114);
        c.drawPath(poly(982, 198, 996, 214, 995, 276, 980, 284, 972, 240), p);
        stroke(0xFFB8BEC6, 2.5f);
        c.drawLine(983, 200, 981, 282, p);
        fill(0xFF141519);
        r.set(934, 250, 976, 278);
        c.drawRoundRect(r, 10, 10, p);
        fill(0xFFDDE3EA);
        c.drawCircle(958, 264, 7, p);
        fill(0xFF0E0F12);
        r.set(890, 286, 990, 296);
        c.drawRoundRect(r, 4, 4, p);

        // Feu arrière phase 2 (débordant sur l'aile)
        tmp.reset();
        tmp.moveTo(36, 150);
        tmp.lineTo(102, 146);
        tmp.lineTo(108, 168);
        tmp.lineTo(40, 178);
        tmp.close();
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(36, 0, 108, 0, new int[]{0xFF5A0006, 0xFFD0101C, 0xFFFF3B3B}, null, Shader.TileMode.CLAMP));
        c.drawPath(tmp, p);
        p.setShader(null);
        stroke(0xFFFF8A8A, 2f);
        c.drawLine(50, 158, 98, 155, p);
        c.drawLine(50, 166, 100, 163, p);
        fill(0xFFB3001B);
        r.set(38, 248, 60, 254);
        c.drawRect(r, p);
        fill(0xFF0E0F12);
        r.set(40, 282, 132, 292);
        c.drawRoundRect(r, 4, 4, p);

        drawWheel(c, RW);
        drawWheel(c, FW);
    }

    private void drawDrl(Canvas c) {
        tmp.reset();
        tmp.moveTo(896, 190);
        tmp.quadTo(924, 195, 944, 189);
        tmp.quadTo(964, 183, 980, 195);
        c.drawPath(tmp, p);
    }

    /** Jante 17" à 5 doubles branches. */
    private void drawWheel(Canvas c, float cx) {
        float cy = WY;
        fill(0xFF141416);
        c.drawCircle(cx, cy, 70, p);
        stroke(0xFF26272B, 3);
        c.drawCircle(cx, cy, 61, p);
        p.setStyle(Paint.Style.FILL);
        p.setShader(new RadialGradient(cx - 12, cy - 14, 58, new int[]{0xFFF2F4F7, 0xFFB9BEC6, 0xFF656A72}, null, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, 51, p);
        p.setShader(null);
        fill(0xFF23262B);
        for (int i = 0; i < 5; i++) {
            double base = Math.toRadians(i * 72 - 90);
            wedge(c, cx, cy, base + Math.toRadians(36), Math.toRadians(17), 17, 46);   // grande ouverture
            wedge(c, cx, cy, base, Math.toRadians(3.2), 22, 44);                        // fente entre 2 branches
        }
        stroke(0xFF9CA2AA, 1.5f);
        c.drawCircle(cx, cy, 47, p);
        fill(0xFFC9CDD3);
        c.drawCircle(cx, cy, 14, p);
        fill(0xFF50555F);
        c.drawCircle(cx, cy, 6, p);
    }

    private void wedge(Canvas c, float cx, float cy, double mid, double half, float r0, float r1) {
        double a0 = mid - half, a1 = mid + half;
        tmp.reset();
        tmp.moveTo(cx + (float) Math.cos(a0) * r0, cy + (float) Math.sin(a0) * r0);
        tmp.lineTo(cx + (float) Math.cos(a0 - half * 0.25) * r1, cy + (float) Math.sin(a0 - half * 0.25) * r1);
        tmp.lineTo(cx + (float) Math.cos(a1 + half * 0.25) * r1, cy + (float) Math.sin(a1 + half * 0.25) * r1);
        tmp.lineTo(cx + (float) Math.cos(a1) * r0, cy + (float) Math.sin(a1) * r0);
        tmp.close();
        c.drawPath(tmp, p);
    }
}
