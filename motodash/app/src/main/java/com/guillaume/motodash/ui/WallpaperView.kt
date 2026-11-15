package com.guillaume.motodash.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import kotlin.math.max

/**
 * Morceau du fond dégradé, dessiné aux coordonnées de l'écran entier :
 * plusieurs fenêtres (cadre autour du GPS, panneau) forment ainsi un seul fond continu.
 * corner : coin arrondi à découper (le GPS apparaît dans l'arrondi).
 */
class WallpaperView(
    ctx: Context,
    private val offX: Float, private val offY: Float,
    private val screenW: Float, private val screenH: Float,
    private val corner: Corner? = null,
) : View(ctx) {
    enum class Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(c: Canvas) {
        if (corner != null) {
            val w = width.toFloat(); val h = height.toFloat()
            val (cx, cy) = when (corner) {
                Corner.TOP_LEFT -> w to h
                Corner.TOP_RIGHT -> 0f to h
                Corner.BOTTOM_LEFT -> w to 0f
                Corner.BOTTOM_RIGHT -> 0f to 0f
            }
            val keep = Path().apply { addRect(0f, 0f, w, h, Path.Direction.CW) }
            val hole = Path().apply { addCircle(cx, cy, w, Path.Direction.CW) }
            keep.op(hole, Path.Op.DIFFERENCE)
            c.clipPath(keep)
        }
        c.translate(-offX, -offY)
        draw(c, screenW, screenH, p)
    }

    companion object {
        /** Même dégradé violet / rouge / bleu que l'écran MotoDash. */
        fun draw(c: Canvas, w: Float, h: Float, p: Paint) {
            c.drawColor(0xFF07070F.toInt())
            val d = max(w, h)
            fun blob(x: Float, y: Float, r: Float, color: Long) {
                p.shader = RadialGradient(x, y, r, color.toInt(), (color and 0x00FFFFFF).toInt(), Shader.TileMode.CLAMP)
                c.drawRect(0f, 0f, w, h, p)
            }
            blob(w * 0.55f, h * 0.45f, d * 0.55f, 0xCC5E1A8C)
            blob(w * 0.9f, h * 0.05f, d * 0.5f, 0xDDB0183F)
            blob(w * 0.1f, h * 1.0f, d * 0.55f, 0xDD2440C8)
            p.shader = null
        }
    }
}
