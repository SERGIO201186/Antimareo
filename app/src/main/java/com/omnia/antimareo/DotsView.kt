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

    /** Velocidad de giro en rad/s, positiva = giro a la izquierda. */
    @Volatile
    var targetYaw = 0f
    private var yaw = 0f
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

        yaw += (targetYaw - yaw) * (dt * 4f).coerceAtMost(1f)
        val dead = 0.03f
        val yawEff = if (kotlin.math.abs(yaw) < dead) 0f else yaw - dead * Math.signum(yaw)
        val tilt = (yawEff * 2.5f * (sens / 1.5f)).coerceIn(-1f, 1f)
        val maxShift = 56f * d

        val w = width.toFloat()
        val h = height.toFloat()
        // Desplazamiento lateral: 0 abajo, máximo arriba, hacia donde gira el vehículo
        fun off(py: Float) = -tilt * maxShift * (1f - (py / h).coerceIn(0f, 1f))
        fun cx(x: Float, py: Float, r: Float) = (x + off(py)).coerceIn(r, w - r)

        var y = -gap + phase
        while (y < h + gap) {
            val y2 = y + gap / 2f
            c.drawCircle(cx(16f * d, y, 8f * d), y, 8f * d, paint)
            c.drawCircle(cx(w - 16f * d, y, 8f * d), y, 8f * d, paint)
            c.drawCircle(cx(44f * d, y2, 5f * d), y2, 5f * d, paint)
            c.drawCircle(cx(w - 44f * d, y2, 5f * d), y2, 5f * d, paint)
            y += gap
        }
        postInvalidateOnAnimation()
    }
}
