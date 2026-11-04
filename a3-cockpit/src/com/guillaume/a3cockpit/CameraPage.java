package com.guillaume.a3cockpit;

import android.widget.FrameLayout;

/** Caméra de recul en grand. */
final class CameraPage extends Page {
    private final CameraView cam;

    CameraPage(final MainActivity a, float w, float h) {
        super(a, w, h);
        setBackground(Ui.card());
        setClipToOutline(true);
        cam = Ui.place(this, new CameraView(a), 0, 0, w, h);
        FrameLayout back = Ui.place(this, new FrameLayout(a), 14, 14, 48, 48);
        back.setBackground(Ui.round(0x99000000, 24));
        Ui.place(back, new IconView(a, IconView.CLOSE, Ui.TEXT), 13, 13, 22, 22);
        Ui.onTap(back, new Runnable() { @Override public void run() { a.showHome(); } });
        cam.start();
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cam.stop();
    }
}
