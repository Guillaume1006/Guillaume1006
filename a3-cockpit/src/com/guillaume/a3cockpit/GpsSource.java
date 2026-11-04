package com.guillaume.a3cockpit;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

/** Vitesse GPS + estimation de la distance parcourue pour le kilométrage. */
final class GpsSource implements LocationListener {
    private final Context ctx;
    private LocationManager lm;
    private Location last;
    private boolean running;

    GpsSource(Context c) { ctx = c.getApplicationContext(); }

    void start() {
        if (running) return;
        if (ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return;
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this, Looper.getMainLooper());
            running = true;
        } catch (Exception ignored) { }
    }

    void stop() {
        if (lm != null && running) {
            try { lm.removeUpdates(this); } catch (Exception ignored) { }
        }
        running = false;
    }

    @Override public void onLocationChanged(Location loc) {
        float kmh;
        if (loc.hasSpeed()) kmh = loc.getSpeed() * 3.6f;
        else if (last != null && loc.getTime() > last.getTime())
            kmh = last.distanceTo(loc) / ((loc.getTime() - last.getTime()) / 1000f) * 3.6f;
        else kmh = 0;
        if (kmh < 2) kmh = 0;
        VehicleState.gpsSpeed = kmh;
        VehicleState.gpsTime = System.currentTimeMillis();
        VehicleState.gpsFix = true;

        if (loc.hasAccuracy() && loc.getAccuracy() > 30) return;
        if (last != null) {
            float d = last.distanceTo(loc);
            long dt = loc.getTime() - last.getTime();
            // On ignore l'immobilité (dérive GPS) et les sauts aberrants.
            if (kmh >= 4 && d > 3 && dt > 0 && d / (dt / 1000f) < 70) Odometer.addMeters(d);
            if (kmh < 4 && d < 30) return;  // garde le point de référence à l'arrêt
        }
        last = loc;
    }

    @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
    @Override public void onProviderEnabled(String provider) { }
    @Override public void onProviderDisabled(String provider) { VehicleState.gpsFix = false; }
}
