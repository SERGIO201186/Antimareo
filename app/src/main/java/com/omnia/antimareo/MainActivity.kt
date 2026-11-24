
package com.omnia.antimareo

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val d = resources.displayMetrics.density
        val pad = (20 * d).toInt()
        val prefs = getSharedPreferences("cfg", Context.MODE_PRIVATE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }
        root.addView(TextView(this).apply {
            text = "AntiMareo"
            textSize = 28f
        })
        root.addView(TextView(this).apply {
            text = "Puntos en los laterales que avanzan con la velocidad del vehículo, sobre cualquier app."
            textSize = 15f
            setPadding(0, pad / 2, 0, pad)
        })
        status = TextView(this).apply {
            textSize = 15f
            setPadding(0, 0, 0, pad)
        }
        root.addView(status)

        root.addView(button("1. Permitir mostrar sobre otras apps") {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + packageName))
            )
        })
        root.addView(button("2. Permitir ubicación y notificaciones") {
            val perms = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
            requestPermissions(perms.toTypedArray(), 1)
        })
        root.addView(button("Iniciar guías") {
            if (!Settings.canDrawOverlays(this)) {
                toast("Falta el permiso de mostrar sobre otras apps")
            } else if (!hasLocation()) {
                toast("Falta el permiso de ubicación")
            } else {
                startForegroundService(Intent(this, OverlayService::class.java))
                toast("Guías activadas")
                refresh()
            }
        })
        root.addView(button("Detener guías") {
            stopService(Intent(this, OverlayService::class.java))
            toast("Guías detenidas")
            refresh()
        })

        root.addView(TextView(this).apply {
            text = "Sensibilidad de velocidad"
            textSize = 16f
            setPadding(0, pad, 0, pad / 4)
        })
        root.addView(SeekBar(this).apply {
            max = 10
            progress = ((prefs.getFloat("sens", 1.5f) - 0.5f) / 0.25f).toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    prefs.edit().putFloat("sens", 0.5f + p * 0.25f).apply()
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })
        root.addView(TextView(this).apply {
            text = "Mira al horizonte, no a los puntos. Esta app es de apoyo y no sustituye consejo médico."
            textSize = 13f
            gravity = Gravity.START
            setPadding(0, pad, 0, 0)
        })
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, res: IntArray) {
        super.onRequestPermissionsResult(code, perms, res)
        refresh()
    }

    private fun hasLocation() =
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun refresh() {
        val running = getSharedPreferences("cfg", Context.MODE_PRIVATE).getBoolean("running", false)
        val ov = if (Settings.canDrawOverlays(this)) "✓" else "✗"
        val loc = if (hasLocation()) "✓" else "✗"
        status.text = "Mostrar sobre otras apps: " + ov + "\nUbicación: " + loc +
            "\nEstado: " + (if (running) "ACTIVO" else "detenido")
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { onClick() }
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
