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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max

data class RideState(
    val speedKmh: Float = 0f,
    val maxKmh: Float = 0f,
    val tripKm: Double = 0.0,
    val altitude: Double? = null,
    val bearing: Float? = null,
    val accuracy: Float? = null,
    val hasFix: Boolean = false,
    val startMs: Long = System.currentTimeMillis(),
)

/** Suit la position GPS : vitesse, vitesse max, distance du trajet, altitude, cap. */
class LocationTracker(private val ctx: Context) : LocationListener {
    private val lm = ctx.getSystemService(LocationManager::class.java)
    private val _state = MutableStateFlow(RideState())
    val state: StateFlow<RideState> = _state
    private var last: Location? = null
    private var running = false

    fun hasPermission() = ContextCompat.checkSelfPermission(
        ctx, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun start() {
        if (running || !hasPermission()) return
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this, Looper.getMainLooper())
            running = true
        } catch (_: Exception) {
            running = false
        }
    }

    fun stop() {
        if (!running) return
        lm.removeUpdates(this)
        running = false
    }

    fun resetTrip() {
        last = null
        _state.update { it.copy(maxKmh = 0f, tripKm = 0.0, startMs = System.currentTimeMillis()) }
    }

    override fun onLocationChanged(loc: Location) {
        val good = loc.accuracy < 30f
        val kmh = if (loc.hasSpeed()) loc.speed * 3.6f else 0f
        var addKm = 0.0
        val prev = last
        if (good) {
            if (prev != null && kmh > 3f) addKm = prev.distanceTo(loc) / 1000.0
            last = loc
        }
        _state.update {
            it.copy(
                speedKmh = kmh,
                maxKmh = if (good) max(it.maxKmh, kmh) else it.maxKmh,
                tripKm = it.tripKm + addKm,
                altitude = if (loc.hasAltitude()) loc.altitude else it.altitude,
                bearing = if (loc.hasBearing() && kmh > 5f) loc.bearing else it.bearing,
                accuracy = loc.accuracy,
                hasFix = true,
            )
        }
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {
        _state.update { it.copy(hasFix = false, speedKmh = 0f) }
    }
    @Deprecated("Requis pour Android < 10")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}
