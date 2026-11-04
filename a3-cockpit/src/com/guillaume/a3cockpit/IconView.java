package com.guillaume.a3cockpit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Icônes dessinées à la main sur une grille 24 x 24 (aucune ressource externe). */
final class IconView extends View {
    static final int NAV = 1, MUSIC = 2, PHONE = 3, GRID = 4, HOME = 5, RADIO = 6, SOUND = 7, GEAR = 8,
            CAR = 9, DOOR = 10, WRENCH = 11, RADAR = 12, PLAY = 13, PAUSE = 14, PREV = 15, NEXT = 16,
            BT = 17, BACK = 18, THERMO = 19, BATTERY = 20, FUEL = 21, ROAD = 22, GAUGE = 23, CHEVRON = 24,
            EDIT = 25, CHECK = 26, DROP = 27, INFO = 28, PLUS = 29, PHOTO = 30, PALETTE = 31, TRUNK = 32, HOOD = 33;

    private int kind;
    private int color;

    IconView(Context c, int kind, int color) {
        super(c);
        this.kind = kind;
        this.color = color;
    }

    void set(int kind, int color) {
        if (this.kind != kind || this.color != color) {
            this.kind = kind;
            this.color = color;
            invalidate();
        }
    }

    @Override protected void onDraw(Canvas c) {
        draw(c, kind, color, getWidth() / 2f, getHeight() / 2f, Math.min(getWidth(), getHeight()));
    }

    private static final Paint P = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path PATH = new Path();
    private static final RectF R = new RectF();

    private static Paint stroke(int color, float w) {
        P.reset();
        P.setAntiAlias(true);
        P.setColor(color);
        P.setStyle(Paint.Style.STROKE);
        P.setStrokeWidth(w);
        P.setStrokeCap(Paint.Cap.ROUND);
        P.setStrokeJoin(Paint.Join.ROUND);
        return P;
    }

    private static Paint fill(int color) {
        P.reset();
        P.setAntiAlias(true);
        P.setColor(color);
        P.setStyle(Paint.Style.FILL);
        return P;
    }

    private static Path poly(float... xy) {
        PATH.reset();
        PATH.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) PATH.lineTo(xy[i], xy[i + 1]);
        return PATH;
    }

    static void draw(Canvas c, int kind, int color, float cx, float cy, float size) {
        if (size <= 0) return;
        c.save();
        float k = size / 24f;
        c.translate(cx - 12 * k, cy - 12 * k);
        c.scale(k, k);
        switch (kind) {
            case NAV:
                poly(20, 4, 3.5f, 11, 11, 13, 13, 20.5f);
                PATH.close();
                c.drawPath(PATH, fill(color));
                break;
            case MUSIC:
                c.drawPath(poly(9, 17.5f, 9, 5.5f, 19, 3.5f, 19, 15.5f), stroke(color, 2.2f));
                c.drawCircle(6.6f, 17.6f, 2.8f, fill(color));
                c.drawCircle(16.6f, 15.6f, 2.8f, fill(color));
                break;
            case PHONE:
                PATH.reset();
                PATH.moveTo(5.5f, 3);
                PATH.lineTo(9, 3);
                PATH.lineTo(10.8f, 7.6f);
                PATH.lineTo(8.4f, 9.3f);
                PATH.quadTo(10.2f, 13.6f, 14.7f, 15.6f);
                PATH.lineTo(16.4f, 13.2f);
                PATH.lineTo(21, 15);
                PATH.lineTo(21, 18.5f);
                PATH.quadTo(21, 21, 18.5f, 21);
                PATH.cubicTo(10, 20.5f, 3.5f, 14, 3, 5.5f);
                PATH.quadTo(3, 3, 5.5f, 3);
                PATH.close();
                c.drawPath(PATH, fill(color));
                break;
            case GRID:
                fill(color);
                R.set(4, 4, 10.5f, 10.5f); c.drawRoundRect(R, 2, 2, P);
                R.set(13.5f, 4, 20, 10.5f); c.drawRoundRect(R, 2, 2, P);
                R.set(4, 13.5f, 10.5f, 20); c.drawRoundRect(R, 2, 2, P);
                R.set(13.5f, 13.5f, 20, 20); c.drawRoundRect(R, 2, 2, P);
                break;
            case HOME:
                c.drawPath(poly(3, 11.5f, 12, 3.5f, 21, 11.5f), stroke(color, 2.2f));
                c.drawPath(poly(5.5f, 10, 5.5f, 20.5f, 18.5f, 20.5f, 18.5f, 10), stroke(color, 2.2f));
                break;
            case RADIO:
                c.drawCircle(12, 12, 2.4f, fill(color));
                stroke(color, 2.2f);
                R.set(6, 6, 18, 18);
                c.drawArc(R, -50, 100, false, P);
                c.drawArc(R, 130, 100, false, P);
                R.set(2, 2, 22, 22);
                c.drawArc(R, -45, 90, false, P);
                c.drawArc(R, 135, 90, false, P);
                break;
            case SOUND:
                stroke(color, 2.2f);
                c.drawLine(6, 4, 6, 20, P);
                c.drawLine(12, 4, 12, 20, P);
                c.drawLine(18, 4, 18, 20, P);
                fill(color);
                c.drawCircle(6, 14, 2.8f, P);
                c.drawCircle(12, 8, 2.8f, P);
                c.drawCircle(18, 15.5f, 2.8f, P);
                break;
            case GEAR:
                stroke(color, 3f);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI / 4 * i;
                    c.drawLine(12 + (float) Math.cos(a) * 7f, 12 + (float) Math.sin(a) * 7f,
                            12 + (float) Math.cos(a) * 9.6f, 12 + (float) Math.sin(a) * 9.6f, P);
                }
                stroke(color, 2.2f);
                c.drawCircle(12, 12, 6.6f, P);
                c.drawCircle(12, 12, 2.6f, P);
                break;
            case CAR:
                stroke(color, 2f);
                c.drawPath(poly(5.5f, 11, 7.6f, 5.8f, 16.4f, 5.8f, 18.5f, 11), P);
                R.set(3, 10.5f, 21, 17.5f);
                c.drawRoundRect(R, 2.5f, 2.5f, P);
                fill(color);
                R.set(5, 17, 8.5f, 20.5f); c.drawRoundRect(R, 1, 1, P);
                R.set(15.5f, 17, 19, 20.5f); c.drawRoundRect(R, 1, 1, P);
                c.drawCircle(7, 14, 1.4f, P);
                c.drawCircle(17, 14, 1.4f, P);
                break;
            case DOOR:
                poly(6, 21, 6, 10, 12, 3.5f, 19, 3.5f, 19, 21);
                PATH.close();
                c.drawPath(PATH, stroke(color, 2.1f));
                c.drawLine(6.5f, 10.5f, 18.5f, 10.5f, P);
                c.drawLine(14, 14, 16.5f, 14, P);
                break;
            case TRUNK:
                stroke(color, 2.1f);
                R.set(3, 9, 21, 19);
                c.drawRoundRect(R, 2.5f, 2.5f, P);
                c.drawPath(poly(5, 9, 7, 4.5f, 17, 4.5f, 19, 9), P);
                c.drawLine(9, 14, 15, 14, P);
                break;
            case HOOD:
                stroke(color, 2.1f);
                R.set(3, 11, 21, 19);
                c.drawRoundRect(R, 2.5f, 2.5f, P);
                c.drawPath(poly(4, 11, 9, 3.5f, 20, 7.5f), P);
                break;
            case WRENCH:
                stroke(color, 3f);
                c.drawLine(5, 19, 12.5f, 11.5f, P);
                stroke(color, 2.2f);
                R.set(11.5f, 3.5f, 20.5f, 12.5f);
                c.drawArc(R, 60, 290, false, P);
                break;
            case RADAR:
                c.drawCircle(12, 19.5f, 2.2f, fill(color));
                stroke(color, 2.2f);
                R.set(7, 14.5f, 17, 24.5f); c.drawArc(R, 225, 90, false, P);
                R.set(3.5f, 11, 20.5f, 28); c.drawArc(R, 228, 84, false, P);
                R.set(0, 7.5f, 24, 31.5f); c.drawArc(R, 231, 78, false, P);
                break;
            case PLAY:
                poly(8, 4.5f, 19.5f, 12, 8, 19.5f);
                PATH.close();
                fill(color);
                P.setPathEffect(new android.graphics.CornerPathEffect(1.5f));
                c.drawPath(PATH, P);
                break;
            case PAUSE:
                fill(color);
                R.set(6.5f, 4.5f, 10.3f, 19.5f); c.drawRoundRect(R, 1.2f, 1.2f, P);
                R.set(13.7f, 4.5f, 17.5f, 19.5f); c.drawRoundRect(R, 1.2f, 1.2f, P);
                break;
            case PREV:
                fill(color);
                poly(12, 6, 3, 12, 12, 18); PATH.close(); c.drawPath(PATH, P);
                poly(21, 6, 12, 12, 21, 18); PATH.close(); c.drawPath(PATH, P);
                break;
            case NEXT:
                fill(color);
                poly(3, 6, 12, 12, 3, 18); PATH.close(); c.drawPath(PATH, P);
                poly(12, 6, 21, 12, 12, 18); PATH.close(); c.drawPath(PATH, P);
                break;
            case BT:
                c.drawPath(poly(6.5f, 7.5f, 17, 16.5f, 12, 21, 12, 3, 17, 7.5f, 6.5f, 16.5f), stroke(color, 2f));
                break;
            case BACK:
                c.drawPath(poly(15, 4.5f, 7.5f, 12, 15, 19.5f), stroke(color, 2.6f));
                break;
            case CHEVRON:
                c.drawPath(poly(9, 4.5f, 16.5f, 12, 9, 19.5f), stroke(color, 2.6f));
                break;
            case THERMO:
                stroke(color, 2f);
                R.set(9.5f, 2.5f, 14.5f, 15);
                c.drawRoundRect(R, 2.5f, 2.5f, P);
                c.drawCircle(12, 17.5f, 4f, fill(color));
                break;
            case BATTERY:
                stroke(color, 2f);
                R.set(3, 7.5f, 21, 19);
                c.drawRoundRect(R, 2.5f, 2.5f, P);
                c.drawLine(7, 13.2f, 10, 13.2f, P);
                c.drawLine(14, 13.2f, 17, 13.2f, P);
                c.drawLine(15.5f, 11.7f, 15.5f, 14.7f, P);
                fill(color);
                R.set(5.5f, 4.5f, 9, 7.5f); c.drawRect(R, P);
                R.set(15, 4.5f, 18.5f, 7.5f); c.drawRect(R, P);
                break;
            case FUEL:
                stroke(color, 2f);
                R.set(4, 3.5f, 13.5f, 20.5f);
                c.drawRoundRect(R, 2, 2, P);
                c.drawPath(poly(13.5f, 9, 16.5f, 9, 18.5f, 11, 18.5f, 17, 20, 18.5f, 20, 7, 17.5f, 4.5f), P);
                c.drawLine(2.5f, 20.5f, 15, 20.5f, P);
                fill(color);
                R.set(6, 5.5f, 11.5f, 10); c.drawRect(R, P);
                break;
            case ROAD:
                stroke(color, 2f);
                c.drawLine(4, 21, 9, 3, P);
                c.drawLine(20, 21, 15, 3, P);
                stroke(color, 2.2f);
                c.drawLine(12, 4, 12, 7, P);
                c.drawLine(12, 10.5f, 12, 13.5f, P);
                c.drawLine(12, 17, 12, 20.5f, P);
                break;
            case GAUGE:
                stroke(color, 2.2f);
                R.set(3, 4, 21, 22);
                c.drawArc(R, 150, 240, false, P);
                c.drawLine(12, 13, 16.5f, 8.5f, P);
                c.drawCircle(12, 13, 1.8f, fill(color));
                break;
            case EDIT:
                stroke(color, 2.2f);
                c.drawPath(poly(4, 20, 4.5f, 16, 15.5f, 5, 19, 8.5f, 8, 19.5f, 4, 20), P);
                c.drawLine(13, 7.5f, 16.5f, 11, P);
                break;
            case CHECK:
                c.drawPath(poly(4.5f, 12.5f, 9.5f, 17.5f, 19.5f, 6.5f), stroke(color, 2.8f));
                break;
            case DROP:
                PATH.reset();
                PATH.moveTo(12, 2.5f);
                PATH.cubicTo(15.5f, 7.5f, 19, 11, 19, 15);
                PATH.cubicTo(19, 19, 15.9f, 21.5f, 12, 21.5f);
                PATH.cubicTo(8.1f, 21.5f, 5, 19, 5, 15);
                PATH.cubicTo(5, 11, 8.5f, 7.5f, 12, 2.5f);
                PATH.close();
                c.drawPath(PATH, fill(color));
                break;
            case INFO:
                stroke(color, 2f);
                c.drawCircle(12, 12, 9, P);
                c.drawLine(12, 11, 12, 16.5f, P);
                c.drawCircle(12, 7.6f, 1.3f, fill(color));
                break;
            case PLUS:
                stroke(color, 2.6f);
                c.drawLine(12, 5, 12, 19, P);
                c.drawLine(5, 12, 19, 12, P);
                break;
            case PHOTO:
                stroke(color, 2f);
                R.set(3, 5, 21, 19);
                c.drawRoundRect(R, 2.5f, 2.5f, P);
                c.drawPath(poly(5, 17, 10, 11.5f, 13.5f, 15, 16, 12.5f, 19.5f, 16.5f), P);
                c.drawCircle(15.5f, 8.8f, 1.6f, fill(color));
                break;
            case PALETTE:
                stroke(color, 2f);
                c.drawCircle(12, 12, 9, P);
                fill(color);
                c.drawCircle(8, 10, 1.6f, P);
                c.drawCircle(12, 7.5f, 1.6f, P);
                c.drawCircle(16, 10, 1.6f, P);
                c.drawCircle(14.5f, 15, 2.2f, P);
                break;
            default:
        }
        P.setPathEffect(null);
        c.restore();
    }
}
