package com.guillaume.a3cockpit;

import android.os.Handler;

import java.util.Arrays;

/** Scénario de démonstration : portes, coffre, capot et radars qui s'animent. */
final class DemoSource implements Runnable {
    private final Handler h;
    private boolean on;
    private int step;
    private float rpmWalk = 850;

    DemoSource(Handler h) { this.h = h; }

    void setEnabled(boolean enabled) {
        if (enabled == on) return;
        on = enabled;
        h.removeCallbacks(this);
        if (on) {
            step = 0;
            h.post(this);
        } else {
            Arrays.fill(VehicleState.openings, VehicleState.UNKNOWN);
            Arrays.fill(VehicleState.radarFront, -1);
            Arrays.fill(VehicleState.radarRear, -1);
            VehicleState.openingsSource = "";
            if ("Démo".equals(VehicleState.dataSource)) {
                VehicleState.rpm = VehicleState.coolant = VehicleState.intake = VehicleState.ambient = Float.NaN;
                VehicleState.battery = VehicleState.fuelPct = VehicleState.fuelL = VehicleState.load = Float.NaN;
                VehicleState.dataSource = "";
            }
        }
    }

    @Override public void run() {
        if (!on) return;
        int[] o = VehicleState.openings;
        Arrays.fill(o, VehicleState.CLOSED);
        int s = step % 22;
        switch (s) {
            case 2: case 3: o[VehicleState.FL] = VehicleState.OPEN; break;
            case 4: o[VehicleState.FL] = VehicleState.OPEN; o[VehicleState.RR] = VehicleState.OPEN; break;
            case 6: case 7: o[VehicleState.TRUNK] = VehicleState.OPEN; break;
            case 9: o[VehicleState.HOOD] = VehicleState.OPEN; break;
            default:
        }
        VehicleState.openingsSource = "Démo";

        int[] rear = {-1, -1, -1, -1};
        int[] front = {-1, -1, -1, -1};
        if (s >= 11 && s <= 19) {
            int base = 160 - (s - 11) * 17;
            rear = new int[]{base + 25, base, base + 8, base + 40};
        }
        if (s == 20) front = new int[]{90, 60, 75, 120};
        System.arraycopy(rear, 0, VehicleState.radarRear, 0, 4);
        System.arraycopy(front, 0, VehicleState.radarFront, 0, 4);
        VehicleState.radarTime = System.currentTimeMillis();

        if (!VehicleState.obdConnected) {
            rpmWalk = Math.max(780, Math.min(2600, rpmWalk + (float) (Math.random() - 0.45) * 260));
            VehicleState.rpm = rpmWalk;
            VehicleState.coolant = 89;
            VehicleState.intake = 12;
            VehicleState.ambient = 4.5f;
            VehicleState.battery = 14.2f + (float) Math.random() * 0.2f;
            VehicleState.fuelPct = 26;
            VehicleState.load = 18;
            VehicleState.dataTime = System.currentTimeMillis();
            VehicleState.dataSource = "Démo";
        }
        step++;
        h.postDelayed(this, 1600);
    }
}
