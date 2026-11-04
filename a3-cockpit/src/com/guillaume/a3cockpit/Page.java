package com.guillaume.a3cockpit;

import android.widget.FrameLayout;

/** Une page de la zone principale (à droite du dock). Dimensions en unités "design". */
abstract class Page extends FrameLayout {
    final MainActivity a;
    final float w, h;

    Page(MainActivity a, float w, float h) {
        super(a);
        this.a = a;
        this.w = w;
        this.h = h;
    }

    /** Appelée ~2 fois par seconde tant que la page est visible. */
    void refresh() { }

    void onShow() { refresh(); }
}
