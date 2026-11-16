package com.guillaume.motodash.ui

import android.content.Context
import com.guillaume.motodash.Apps

/** Ouvre le GPS dans l'autre moitié de l'écran partagé Android. */
object Drive {
    fun start(ctx: Context, state: DashState) {
        val nav = state.navApp ?: return
        Apps.launch(ctx, nav, adjacent = true)
    }
}
