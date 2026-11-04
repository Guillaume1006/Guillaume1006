package com.guillaume.a3cockpit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

/**
 * Page Ouvrants : rendu de l'A3 vue de dessus en 3/4 arrière, avec un repère rouge lumineux
 * sur chaque ouvrant ouvert. Positions des repères en fraction de l'image.
 */
final class OpeningsView extends View {
    /** x, y (0..1) pour FL, FR, RL, RR, coffre, capot. */
    private static final float[][] POS = {
            {0.22f, 0.43f}, {0.60f, 0.19f}, {0.40f, 0.58f}, {0.77f, 0.33f}, {0.82f, 0.61f}, {0.11f, 0.13f}};
    private static final String[] TAG = {"AV G", "AV D", "AR G", "AR D", "Coffre", "Capot"};

    private final Bitmap img;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final float[] anim = new float[6];
    private long t0 = System.currentTimeMillis();

    OpeningsView(Context c, Bitmap img) {
        super(c);
        this.img = img;
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        glow.setMaskFilter(new BlurMaskFilter(Ui.f(14), BlurMaskFilter.Blur.NORMAL));
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0 || img == null) return;
        float ar = img.getWidth() / (float) img.getHeight();
        float iw = w, ih = w / ar;
        if (ih > h * 0.92f) { ih = h * 0.92f; iw = ih * ar; }
        float x0 = (w - iw) / 2f, y0 = (h - ih) / 2f;

        // ombre au sol
        p.setShader(new RadialGradient(0, 0, 1, 0xB0000000, 0x00000000, Shader.TileMode.CLAMP));
        c.save();
        c.translate(x0 + iw * 0.52f, y0 + ih * 0.58f);
        c.rotate(30);
        c.scale(iw * 0.58f, ih * 0.36f);
        c.drawCircle(0, 0, 1, p);
        c.restore();
        p.setShader(null);

        r.set(x0, y0, x0 + iw, y0 + ih);
        c.drawBitmap(img, null, r, p);

        boolean five = Prefs.fiveDoors();
        boolean animating = false;
        float pulse = 0.5f + 0.5f * (float) Math.sin((System.currentTimeMillis() - t0) / 260.0);
        for (int i = 0; i < 6; i++) {
            if (!five && (i == VehicleState.RL || i == VehicleState.RR)) continue;
            float target = VehicleState.openings[i] == VehicleState.OPEN ? 1f : 0f;
            anim[i] += (target - anim[i]) * 0.2f;
            if (Math.abs(anim[i] - target) < 0.01f) anim[i] = target;
            float a = anim[i];
            if (a <= 0.01f) continue;
            animating = true;
            float cx = x0 + POS[i][0] * iw, cy = y0 + POS[i][1] * ih;
            float rad = Ui.f(16) * (0.85f + 0.3f * pulse);
            glow.setColor(Ui.alpha(Ui.RED, (int) (200 * a)));
            c.drawCircle(cx, cy, rad * 1.8f, glow);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Ui.alpha(Ui.RED, (int) (235 * a)));
            c.drawCircle(cx, cy, rad * 0.62f, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Ui.f(2.5f));
            p.setColor(Ui.alpha(0xFFFFFFFF, (int) (230 * a)));
            c.drawCircle(cx, cy, rad * 0.62f, p);
            // étiquette
            p.setStyle(Paint.Style.FILL);
            p.setTypeface(Ui.MED);
            p.setTextSize(Ui.f(13));
            String tag = five || i > 1 ? TAG[i] : (i == 0 ? "Porte G" : "Porte D");
            float tw = p.measureText(tag);
            r.set(cx + rad, cy - Ui.f(12), cx + rad + tw + Ui.f(16), cy + Ui.f(12));
            if (r.right > w) r.offset(-(tw + Ui.f(16) + 2 * rad), 0);
            p.setColor(Ui.alpha(0xE0000000, (int) (220 * a)));
            c.drawRoundRect(r, Ui.f(12), Ui.f(12), p);
            p.setColor(Ui.alpha(0xFFFFFFFF, (int) (255 * a)));
            c.drawText(tag, r.left + Ui.f(8), cy + Ui.f(4.5f), p);
        }
        p.setTypeface(Typeface.DEFAULT);
        if (animating) postInvalidateOnAnimation();
    }
}
