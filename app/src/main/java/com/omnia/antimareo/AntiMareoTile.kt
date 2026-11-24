package com.omnia.antimareo

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class AntiMareoTile : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refresh(isRunning())
    }

    override fun onClick() {
        super.onClick()
        if (isRunning()) {
            stopService(Intent(this, OverlayService::class.java))
            refresh(false)
            return
        }
        val ok = Settings.canDrawOverlays(this) &&
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!ok) {
            openApp()
            return
        }
        try {
            startForegroundService(Intent(this, OverlayService::class.java))
            refresh(true)
        } catch (e: Exception) {
            // Si el sistema no permite iniciarlo desde aqui, abre la app
            openApp()
        }
    }

    private fun isRunning() =
        getSharedPreferences("cfg", Context.MODE_PRIVATE).getBoolean("running", false)

    private fun refresh(on: Boolean) {
        val t = qsTile ?: return
        t.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        t.label = "AntiMareo"
        if (Build.VERSION.SDK_INT >= 29) t.subtitle = if (on) "Activo" else "Apagado"
        t.updateTile()
    }

    private fun openApp() {
        val i = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 2, i, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(i)
        }
    }
}
