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

/** Compacte 3/5 portes vue de profil, posée sur un plateau, dessinée en vectoriel. */
final class CarSideView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Path body = new Path(), dlo = new Path(), tmp = new Path();
    private final RectF r = new RectF();
    private int color = 0xFFC4C9CF;
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
        invalidate();
    }

    /** Petit reflet lumineux qui balaie la carrosserie. */
    void shine() {
        sweep = 0f;
        postInvalidateOnAnimation();
    }

    private void buildPaths() {
        // Profil de compacte (sol à y = 332, roues r = 70 centrées à y = 262)
        body.reset();
        body.moveTo(62, 294);
        body.lineTo(42, 266);
        body.quadTo(32, 225, 36, 185);
        body.lineTo(44, 150);
        body.quadTo(52, 104, 90, 62);
        body.quadTo(112, 38, 166, 29);
        body.quadTo(330, 10, 470, 19);
        body.quadTo(516, 23, 548, 38);
        body.lineTo(668, 120);
        body.quadTo(712, 134, 780, 142);
        body.lineTo(915, 160);
        body.quadTo(960, 167, 976, 186);
        body.quadTo(992, 210, 990, 245);
        body.quadTo(988, 286, 960, 296);
        body.lineTo(882, 294);
        r.set(720, 182, 880, 342);
        body.arcTo(r, 0, -180, false);
        body.lineTo(718, 294);
        body.lineTo(297, 294);
        r.set(135, 182, 295, 342);
        body.arcTo(r, 0, -180, false);
        body.lineTo(133, 294);
        body.close();

        dlo.reset();
        dlo.moveTo(148, 128);
        dlo.quadTo(158, 86, 184, 56);
        dlo.quadTo(198, 43, 226, 41);
        dlo.quadTo(340, 27, 462, 33);
        dlo.quadTo(504, 36, 532, 50);
        dlo.lineTo(640, 130);
        dlo.close();
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float k = Math.min(w / 1100f, h / 400f);
        c.save();
        c.translate(w / 2f - 500 * k, h / 2f - 205 * k);
        c.scale(k, k);
        drawTurntable(c);
        if (photo != null) drawPhoto(c);
        else drawCar(c);
        c.restore();

        if (sweep >= 0f) {
            sweep += 0.025f;
            if (sweep > 1.2f) sweep = -1f;
            postInvalidateOnAnimation();
        }
    }

    private void drawTurntable(Canvas c) {
        p.setShader(new RadialGradient(500, 344, 540, new int[]{0x99000000, 0x66101018, 0x00000000},
                new float[]{0f, 0.7f, 1f}, Shader.TileMode.CLAMP));
        p.setStyle(Paint.Style.FILL);
        r.set(-40, 292, 1040, 396);
        c.drawOval(r, p);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2.5f);
        p.setShader(new LinearGradient(-40, 0, 1040, 0, new int[]{0x00FFFFFF, 0x66FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
        c.drawOval(r, p);
        r.set(40, 308, 960, 380);
        p.setShader(new LinearGradient(40, 0, 960, 0, new int[]{0x00FFFFFF, 0x30FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
        c.drawOval(r, p);
        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawPhoto(Canvas c) {
        float maxW = 1000, maxH = 300;
        float s = Math.min(maxW / photo.getWidth(), maxH / photo.getHeight());
        float bw = photo.getWidth() * s, bh = photo.getHeight() * s;
        r.set(500 - bw / 2, 340 - bh, 500 + bw / 2, 340);
        c.drawBitmap(photo, null, r, p);
    }

    private void drawCar(Canvas c) {
        int light = Ui.blend(color, 0xFFFFFFFF, 0.38f);
        int dark = Ui.blend(color, 0xFF000000, 0.5f);
        int darker = Ui.blend(color, 0xFF000000, 0.72f);

        // Ombre au sol
        p.setShader(new RadialGradient(0, 0, 1, 0xCC000000, 0x00000000, Shader.TileMode.CLAMP));
        c.save();
        c.translate(500, 334);
        c.scale(510, 24);
        c.drawCircle(0, 0, 1, p);
        c.restore();
        p.setShader(null);

        // Passages de roue sombres
        p.setColor(0xFF07070A);
        r.set(720, 182, 880, 342);
        c.drawArc(r, 180, 180, true, p);
        r.set(135, 182, 295, 342);
        c.drawArc(r, 180, 180, true, p);

        // Carrosserie
        p.setShader(new LinearGradient(0, 16, 0, 298,
                new int[]{light, color, color, dark, darker}, new float[]{0f, 0.4f, 0.58f, 0.86f, 1f}, Shader.TileMode.CLAMP));
        c.drawPath(body, p);
        p.setShader(null);

        c.save();
        c.clipPath(body);
        p.setColor(0x55000000);                       // bas de caisse
        c.drawRect(0, 266, 1000, 300, p);
        p.setShader(new LinearGradient(0, 140, 0, 178, new int[]{0x00FFFFFF, 0x40FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
        c.drawRect(0, 140, 1000, 178, p);             // reflet horizontal
        p.setShader(null);
        if (sweep >= 0f) {                            // reflet animé
            float x = -200 + sweep * 1400;
            p.setShader(new LinearGradient(x - 90, 0, x + 90, 0, new int[]{0x00FFFFFF, 0x55FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
            tmp.reset();
            tmp.moveTo(x - 60, 0);
            tmp.lineTo(x + 40, 0);
            tmp.lineTo(x - 20, 300);
            tmp.lineTo(x - 120, 300);
            tmp.close();
            c.drawPath(tmp, p);
            p.setShader(null);
        }
        c.restore();

        // Lunette arrière (vue de biais)
        tmp.reset();
        tmp.moveTo(56, 132);
        tmp.quadTo(66, 92, 100, 62);
        tmp.lineTo(140, 44);
        tmp.lineTo(146, 54);
        tmp.quadTo(108, 74, 90, 104);
        tmp.lineTo(74, 134);
        tmp.close();
        p.setColor(0xFF12161D);
        c.drawPath(tmp, p);

        // Becquet
        tmp.reset();
        tmp.moveTo(166, 29);
        tmp.quadTo(120, 34, 92, 58);
        tmp.lineTo(100, 62);
        tmp.quadTo(124, 42, 168, 36);
        tmp.close();
        p.setColor(darker);
        c.drawPath(tmp, p);

        // Ligne de caisse
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2.2f);
        p.setColor(0x55FFFFFF);
        c.drawLine(40, 164, 945, 174, p);
        p.setColor(0x40000000);
        c.drawLine(40, 168, 945, 178, p);
        p.setStyle(Paint.Style.FILL);

        // Vitrage
        p.setShader(new LinearGradient(0, 32, 0, 130, new int[]{0xFF3A4352, 0xFF141820, 0xFF0B0D12}, null, Shader.TileMode.CLAMP));
        c.drawPath(dlo, p);
        p.setShader(null);
        c.save();
        c.clipPath(dlo);
        p.setColor(0x1CFFFFFF);
        tmp.reset();
        tmp.moveTo(300, 26);
        tmp.lineTo(384, 26);
        tmp.lineTo(330, 134);
        tmp.lineTo(246, 134);
        tmp.close();
        c.drawPath(tmp, p);
        tmp.reset();
        tmp.moveTo(520, 26);
        tmp.lineTo(546, 26);
        tmp.lineTo(492, 134);
        tmp.lineTo(466, 134);
        tmp.close();
        c.drawPath(tmp, p);
        c.restore();

        // Montants
        p.setColor(darker);
        tmp.reset();
        if (five) {
            c.drawRect(394, 30, 410, 130, p);            // pied milieu
            tmp.moveTo(226, 128);                         // custode
            tmp.lineTo(246, 42);
            tmp.lineTo(268, 41);
            tmp.lineTo(256, 128);
        } else {
            tmp.moveTo(296, 128);
            tmp.lineTo(314, 37);
            tmp.lineTo(336, 36);
            tmp.lineTo(326, 128);
        }
        tmp.close();
        c.drawPath(tmp, p);
        p.setStyle(Paint.Style.STROKE);                   // joint de vitre
        p.setStrokeWidth(3);
        p.setColor(0xFF0A0A0C);
        c.drawPath(dlo, p);

        // Découpes de portes
        p.setStrokeWidth(2f);
        p.setColor(0x80000000);
        c.drawLine(650, 130, 658, 292, p);
        tmp.reset();
        if (five) {
            c.drawLine(402, 130, 404, 292, p);
            tmp.moveTo(256, 128);
            tmp.quadTo(268, 196, 300, 236);
        } else {
            tmp.moveTo(326, 128);
            tmp.quadTo(336, 200, 330, 276);
        }
        c.drawPath(tmp, p);
        p.setStyle(Paint.Style.FILL);

        // Poignées
        p.setColor(Ui.blend(color, 0xFF000000, 0.3f));
        r.set(566, 150, 602, 158);
        c.drawRoundRect(r, 4, 4, p);
        if (five) {
            r.set(330, 148, 364, 156);
            c.drawRoundRect(r, 4, 4, p);
        }

        // Rétroviseur
        tmp.reset();
        tmp.moveTo(604, 104);
        tmp.quadTo(626, 94, 648, 102);
        tmp.lineTo(654, 126);
        tmp.lineTo(612, 128);
        tmp.close();
        p.setColor(dark);
        c.drawPath(tmp, p);

        // Phare
        tmp.reset();
        tmp.moveTo(882, 160);
        tmp.lineTo(962, 173);
        tmp.quadTo(975, 182, 978, 194);
        tmp.lineTo(896, 186);
        tmp.close();
        p.setShader(new LinearGradient(882, 0, 978, 0, new int[]{0xFF9AA4B2, 0xFFEFF4FF}, null, Shader.TileMode.CLAMP));
        c.drawPath(tmp, p);
        p.setShader(null);
        p.setColor(0xFFFFFFFF);
        p.setStrokeWidth(3);
        p.setStyle(Paint.Style.STROKE);
        c.drawLine(900, 182, 964, 188, p);
        p.setStyle(Paint.Style.FILL);

        // Calandre et entrée d'air
        tmp.reset();
        tmp.moveTo(978, 196);
        tmp.lineTo(992, 212);
        tmp.lineTo(991, 262);
        tmp.lineTo(976, 270);
        tmp.lineTo(966, 238);
        tmp.close();
        p.setColor(0xFF101114);
        c.drawPath(tmp, p);
        r.set(924, 248, 966, 262);
        c.drawRoundRect(r, 6, 6, p);

        // Feu arrière
        tmp.reset();
        tmp.moveTo(36, 150);
        tmp.lineTo(92, 145);
        tmp.lineTo(96, 170);
        tmp.lineTo(38, 178);
        tmp.close();
        p.setShader(new LinearGradient(36, 0, 96, 0, new int[]{0xFF7A0008, 0xFFFF2A2A}, null, Shader.TileMode.CLAMP));
        c.drawPath(tmp, p);
        p.setShader(null);
        p.setColor(0xFFB3001B);
        r.set(38, 244, 58, 250);
        c.drawRect(r, p);

        drawWheel(c, 215);
        drawWheel(c, 800);
    }

    private void drawWheel(Canvas c, float cx) {
        float cy = 262;
        p.setColor(0xFF141416);
        c.drawCircle(cx, cy, 70, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3);
        p.setColor(0xFF26272B);
        c.drawCircle(cx, cy, 60, p);
        p.setStyle(Paint.Style.FILL);
        p.setShader(new RadialGradient(cx - 10, cy - 12, 56, new int[]{0xFFF2F4F7, 0xFFB9BEC6, 0xFF6E737B}, null, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, 49, p);
        p.setShader(null);
        // Disque de frein visible entre les branches
        p.setColor(0xFF2B2E34);
        for (int i = 0; i < 5; i++) {
            double a0 = Math.toRadians(i * 72 + 36 - 22), a1 = Math.toRadians(i * 72 + 36 + 22);
            tmp.reset();
            tmp.moveTo(cx + (float) Math.cos(a0) * 15, cy + (float) Math.sin(a0) * 15);
            tmp.lineTo(cx + (float) Math.cos(a0) * 44, cy + (float) Math.sin(a0) * 44);
            tmp.lineTo(cx + (float) Math.cos(a1) * 44, cy + (float) Math.sin(a1) * 44);
            tmp.lineTo(cx + (float) Math.cos(a1) * 15, cy + (float) Math.sin(a1) * 15);
            tmp.close();
            c.drawPath(tmp, p);
        }
        p.setColor(0xFFC9CDD3);
        c.drawCircle(cx, cy, 12, p);
        p.setColor(0xFF505560);
        c.drawCircle(cx, cy, 5, p);
    }

}
