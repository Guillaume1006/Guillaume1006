package com.guillaume.a3cockpit;

import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Barre verticale de gauche : heure, raccourcis et tiroir d'applis. */
final class Dock extends FrameLayout {
    private final TextView clock;
    private final IconView bt;
    private final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm", Locale.FRANCE);

    Dock(final MainActivity a, float height) {
        super(a);
        setBackground(Ui.card());
        clock = Ui.text(a, "", 19, Ui.TEXT, Ui.BOLD);
        clock.setGravity(Gravity.CENTER);
        Ui.place(this, clock, 0, 16, 68, 24);
        Ui.onTap(clock, new Runnable() { @Override public void run() { a.showHome(); } });
        bt = Ui.place(this, new IconView(a, IconView.BT, Ui.TEXT3), 26, 46, 16, 16);

        button(a, IconView.NAV, Ui.BLUE, 0xFF3F9BFF, 80, new Runnable() { @Override public void run() { Apps.open(a, Apps.NAV); } });
        button(a, IconView.MUSIC, Ui.PINK, 0xFFFF6B8B, 142, new Runnable() { @Override public void run() { Apps.open(a, Apps.MUSIC); } });
        button(a, IconView.PHONE, Ui.GREEN, 0xFF5BE07F, 204, new Runnable() { @Override public void run() { Apps.open(a, Apps.PHONE); } });
        button(a, IconView.CAR, Ui.ORANGE, 0xFFFF8A5C, 266, new Runnable() { @Override public void run() { a.showVehicle(-1); } });

        FrameLayout grid = button(a, IconView.GRID, 0x33FFFFFF, 0x22FFFFFF, height - 64, new Runnable() {
            @Override public void run() { a.showApps(); }
        });
        grid.setLongClickable(true);
    }

    private FrameLayout button(MainActivity a, int icon, int c1, int c2, float y, Runnable r) {
        FrameLayout b = new FrameLayout(a);
        b.setBackground(Ui.gradient(c2, c1, 15));
        Ui.place(b, new IconView(a, icon, Ui.TEXT), 12, 12, 24, 24);
        Ui.place(this, b, 10, y, 48, 48);
        Ui.onTap(b, r);
        return b;
    }

    void refresh() {
        clock.setText(fmt.format(new Date()));
        bt.set(IconView.BT, VehicleState.obdConnected ? Ui.BLUE : Ui.TEXT3);
    }
}
