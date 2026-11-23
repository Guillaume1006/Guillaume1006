package com.guillaume.motodash

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat

/** Position GPS toutes les secondes (sans services Google). */
class LocationTracker(private val ctx: Context, private val onLocation: (Location) -> Unit) : LocationListener {
    private val lm = ctx.getSystemService(LocationManager::class.java)
    private var running = false

    fun hasPermission() = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun start() {
        if (running || !hasPermission()) return
        runCatching {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this, Looper.getMainLooper())
            // Première position approximative pour centrer la carte tout de suite
            (lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER))?.let(onLocation)
            running = true
        }
    }

    fun stop() { if (running) lm.removeUpdates(this); running = false }

    private var prev: Location? = null

    override fun onLocationChanged(loc: Location) {
        // Si le GPS ne donne pas la vitesse ou le cap, on les calcule depuis la position précédente
        val p = prev
        if (p != null) {
            val dt = (loc.time - p.time) / 1000f
            val d = p.distanceTo(loc)
            val measured = if (dt in 0.2f..10f) d / dt else 0f
            // Vitesse absente, ou annoncée à 0 alors qu'on se déplace nettement (> 7 km/h)
            if ((!loc.hasSpeed() || loc.speed < 0.5f) && measured > 2f) loc.speed = measured
            if ((!loc.hasBearing() || (loc.bearing == 0f && measured > 2f)) && d > 3f) loc.bearing = p.bearingTo(loc).let { if (it < 0) it + 360f else it }
        }
        prev = loc
        onLocation(loc)
    }
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Requis pour Android < 10")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}
