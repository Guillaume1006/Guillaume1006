package com.guillaume.a3cockpit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/** Fond « coucher de soleil » avec reliefs, dessiné une fois pour toutes. */
final class BackgroundView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path hills = new Path();

    BackgroundView(Context c) { super(c); }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        if (Ui.MMI) {                                   // noir profond, léger voile gris en haut
            p.setShader(new LinearGradient(0, 0, 0, h, new int[]{0xFF101113, 0xFF050506, 0xFF000000},
                    new float[]{0f, 0.35f, 1f}, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, p);
            p.setShader(null);
            return;
        }
        p.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{0xFF0E0B1C, 0xFF2A1C47, 0xFF6B3462, 0xFFD2603F, 0xFF2A1426},
                new float[]{0f, 0.38f, 0.62f, 0.8f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        p.setShader(new RadialGradient(w * 0.62f, h * 0.8f, w * 0.55f,
                new int[]{0x99FF9A4D, 0x33FF6A3D, 0x00000000}, new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);

        hills.reset();
        hills.moveTo(0, h * 0.78f);
        hills.cubicTo(w * 0.15f, h * 0.70f, w * 0.28f, h * 0.74f, w * 0.40f, h * 0.79f);
        hills.cubicTo(w * 0.52f, h * 0.84f, w * 0.66f, h * 0.72f, w * 0.80f, h * 0.76f);
        hills.cubicTo(w * 0.90f, h * 0.79f, w * 0.96f, h * 0.75f, w, h * 0.77f);
        hills.lineTo(w, h);
        hills.lineTo(0, h);
        hills.close();
        p.setColor(0xCC1C0F24);
        c.drawPath(hills, p);
        hills.reset();
        hills.moveTo(0, h * 0.88f);
        hills.cubicTo(w * 0.2f, h * 0.82f, w * 0.45f, h * 0.9f, w * 0.7f, h * 0.86f);
        hills.cubicTo(w * 0.85f, h * 0.84f, w * 0.95f, h * 0.88f, w, h * 0.87f);
        hills.lineTo(w, h);
        hills.lineTo(0, h);
        hills.close();
        p.setColor(0xFF120A18);
        c.drawPath(hills, p);
        // léger voile pour la lisibilité
        p.setColor(0x33000000);
        c.drawRect(0, 0, w, h, p);
    }
}
