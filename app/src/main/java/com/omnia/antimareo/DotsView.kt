package com.omnia.antimareo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class DotsView(ctx: Context) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(215, 61, 220, 159) }
    private val prefs = ctx.getSharedPreferences("cfg", Context.MODE_PRIVATE)
    private val d = ctx.resources.displayMetrics.density
    private val gap = 64f * d

    @Volatile
    var targetSpeed = 0f
    private var speed = 0f
    private var phase = 0f
    private var last = 0L

    override fun onDraw(c: Canvas) {
        val now = System.nanoTime()
        val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.1f)
        last = now
        speed += (targetSpeed - speed) * (dt * 2f).coerceAtMost(1f)
        val sens = prefs.getFloat("sens", 1.5f)
        val pxs = (speed * 7f * sens * d).coerceAtMost(900f * d)
        phase = (phase + pxs * dt) % gap

        val w = width.toFloat()
        val h = height.toFloat()
        var y = -gap + phase
        while (y < h + gap) {
            c.drawCircle(16f * d, y, 8f * d, paint)
            c.drawCircle(w - 16f * d, y, 8f * d, paint)
            c.drawCircle(44f * d, y + gap / 2f, 5f * d, paint)
            c.drawCircle(w - 44f * d, y + gap / 2f, 5f * d, paint)
            y += gap
        }
        postInvalidateOnAnimation()
    }
}

