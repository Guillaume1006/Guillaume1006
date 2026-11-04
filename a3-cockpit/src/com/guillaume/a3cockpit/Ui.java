package com.guillaume.a3cockpit;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Locale;

/** Petits utilitaires d'interface : tout est dessiné sur une grille de 1024 x 600 mise à l'échelle. */
final class Ui {
    private Ui() {}

    static float scale = 1f;
    static float W = 1024f, H = 600f;

    static int s(float v) { return Math.round(v * scale); }
    static float f(float v) { return v * scale; }

    static final int TEXT = 0xFFFFFFFF;
    static int TEXT2 = 0xB8FFFFFF, TEXT3 = 0x73FFFFFF;
    static int CARD = 0xA617121F, CARD_STROKE = 0x24FFFFFF, CHIP = 0x1CFFFFFF, CHIP_ON = 0x38FFFFFF;
    static final int AUDI_RED = 0xFFD8001E, MMI_BLUE = 0xFF2E9BFF;

    /** Thème : "mmi" (noir façon Audi MMI, par défaut) ou "sunset" (coucher de soleil). */
    static boolean MMI = true;
    static float RADIUS = 1f;          // facteur appliqué aux arrondis

    static void applyTheme(String name) {
        MMI = !"sunset".equals(name);
        if (MMI) {
            TEXT2 = 0xFFA9ADB3; TEXT3 = 0xFF6C7077;
            CARD = 0xF00B0C0E; CARD_STROKE = 0x26FFFFFF; CHIP = 0x14FFFFFF; CHIP_ON = 0x30FFFFFF;
            RADIUS = 0.35f;
        } else {
            TEXT2 = 0xB8FFFFFF; TEXT3 = 0x73FFFFFF;
            CARD = 0xA617121F; CARD_STROKE = 0x24FFFFFF; CHIP = 0x1CFFFFFF; CHIP_ON = 0x38FFFFFF;
            RADIUS = 1f;
        }
    }
    static final int GREEN = 0xFF30D158, RED = 0xFFFF453A, ORANGE = 0xFFFF5F2E, YELLOW = 0xFFFFD60A,
            BLUE = 0xFF0A84FF, PINK = 0xFFFF375F, PURPLE = 0xFF7B61FF, TEAL = 0xFF5AC8FA,
            LIME = 0xFFB8D432, GRAY = 0xFF8E8E93, AMBER = 0xFFFF9F0A;

    static final Typeface REG = Typeface.create("sans-serif", Typeface.NORMAL);
    static final Typeface MED = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    static final Typeface BOLD = Typeface.create("sans-serif", Typeface.BOLD);
    static final Typeface LIGHT = Typeface.create("sans-serif-light", Typeface.NORMAL);

    static GradientDrawable round(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(f(radius * RADIUS));
        return g;
    }

    static GradientDrawable card() {
        GradientDrawable g = round(CARD, 26);
        g.setStroke(Math.max(1, s(1)), CARD_STROKE);
        return g;
    }

    static GradientDrawable gradient(int top, int bottom, float radius) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{top, bottom});
        g.setCornerRadius(f(radius * RADIUS));
        return g;
    }

    static TextView text(Context c, CharSequence s, float size, int color, Typeface tf) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, f(size));
        t.setTextColor(color);
        t.setTypeface(tf);
        t.setIncludeFontPadding(false);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    /** Positionne une vue en coordonnées "design" dans un FrameLayout. w/h < 0 = wrap_content. */
    static <T extends View> T place(ViewGroup parent, T v, float x, float y, float w, float h) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                w < 0 ? ViewGroup.LayoutParams.WRAP_CONTENT : s(w),
                h < 0 ? ViewGroup.LayoutParams.WRAP_CONTENT : s(h));
        lp.leftMargin = s(x);
        lp.topMargin = s(y);
        lp.gravity = Gravity.TOP | Gravity.START;
        parent.addView(v, lp);
        return v;
    }

    static void onTap(final View v, final Runnable r) {
        v.setClickable(true);
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View view, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(70).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        view.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
                        break;
                    default:
                }
                return false;
            }
        });
        v.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { r.run(); }
        });
    }

    static int blend(int a, int b, float t) {
        float u = 1f - t;
        return Color.argb(
                Math.round(Color.alpha(a) * u + Color.alpha(b) * t),
                Math.round(Color.red(a) * u + Color.red(b) * t),
                Math.round(Color.green(a) * u + Color.green(b) * t),
                Math.round(Color.blue(a) * u + Color.blue(b) * t));
    }

    static int alpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    static String num(long v) {
        return String.format(Locale.FRANCE, "%,d", v).replace(' ', ' ').replace(' ', ' ');
    }

    static String dec1(float v) {
        return String.format(Locale.FRANCE, "%.1f", v);
    }

    /** "41" en gros suivi de " km/h" plus petit et plus pâle. */
    static CharSequence valueUnit(String value, String unit) {
        if (unit == null || unit.isEmpty()) return value;
        SpannableString sp = new SpannableString(value + " " + unit);
        int start = value.length();
        sp.setSpan(new RelativeSizeSpan(0.5f), start, sp.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sp.setSpan(new ForegroundColorSpan(TEXT2), start, sp.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sp;
    }

    static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(Locale.FRANCE) + s.substring(1);
    }
}
