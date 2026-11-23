package com.guillaume.motodash.nav

import android.content.Context
import android.location.Location
import com.guillaume.motodash.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

enum class Status { IDLE, ROUTING, NAVIGATING, ARRIVED, ERROR }

data class NavState(
    val status: Status = Status.IDLE,
    val destination: Place? = null,
    val route: Route? = null,
    val next: Maneuver? = null,
    val distToNext: Double = 0.0,
    val remainingM: Double = 0.0,
    val remainingS: Double = 0.0,
    val rerouting: Boolean = false,
    val error: String? = null,
)

/**
 * Le guidage : calcule l'itinéraire, suit la position sur la ligne, trouve la prochaine manœuvre,
 * annonce les consignes et recalcule si on sort de la route.
 */
class Navigator(ctx: Context, private val prefs: Prefs, private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(NavState())
    val state: StateFlow<NavState> = _state
    private val voice = Voice(ctx)

    private var lastLoc: Location? = null
    private var segIdx = 0
    private var offCount = 0
    private var lastReroute = 0L
    private var announcedFar = -1
    private var announcedNear = -1
    private var job: Job? = null

    val position: LatLon? get() = lastLoc?.let { LatLon(it.latitude, it.longitude) }

    fun start(dest: Place) {
        prefs.addRecent(dest)
        _state.value = NavState(Status.ROUTING, dest)
        compute(dest, reroute = false)
    }

    fun stop() {
        job?.cancel()
        voice.stop()
        _state.value = NavState()
    }

    fun shutdown() { job?.cancel(); voice.shutdown() }

    private fun compute(dest: Place, reroute: Boolean) {
        val from = lastLoc
        if (from == null) {
            _state.update { it.copy(status = Status.ERROR, error = "En attente du signal GPS…") }
            pendingDest = dest
            return
        }
        job?.cancel()
        job = scope.launch {
            try {
                val heading = if (from.hasBearing() && from.speed > 2f) from.bearing else null
                val r = withContext(Dispatchers.IO) {
                    Valhalla.route(LatLon(from.latitude, from.longitude), LatLon(dest.lat, dest.lon), prefs.avoidHighways, prefs.avoidTolls, heading)
                }
                segIdx = 0; offCount = 0; announcedFar = -1; announcedNear = -1
                _state.value = NavState(Status.NAVIGATING, dest, r)
                if (prefs.voice) {
                    val first = r.maneuvers.firstOrNull()
                    voice.say(if (reroute) "Nouvel itinéraire." else "C'est parti. " + (first?.pre ?: first?.instruction ?: ""))
                }
                lastLoc?.let { onLocation(it) }
            } catch (e: Exception) {
                if (reroute) _state.update { it.copy(rerouting = false) }
                else _state.update { it.copy(status = Status.ERROR, error = e.message ?: "Pas de connexion") }
            }
        }
    }

    private var pendingDest: Place? = null

    fun onLocation(loc: Location) {
        lastLoc = loc
        // Destination choisie avant d'avoir le GPS : on calcule dès le premier point
        pendingDest?.let { pendingDest = null; _state.value = NavState(Status.ROUTING, it); compute(it, reroute = false); return }

        val s = _state.value
        val r = s.route ?: return
        if (s.status != Status.NAVIGATING || r.points.size < 2) return
        val p = LatLon(loc.latitude, loc.longitude)

        // Point le plus proche sur la ligne, autour de la dernière position connue
        var best = -1; var bestD = Double.MAX_VALUE; var bestT = 0.0
        val lo = max(0, segIdx - 3)
        val hi = min(r.points.size - 2, segIdx + 300)
        for (i in lo..hi) {
            val (t, d) = project(p, r.points[i], r.points[i + 1])
            if (d < bestD) { bestD = d; best = i; bestT = t }
        }
        if (best < 0) return

        // Sortie de route : recalcul après 3 points d'affilée à plus de 50 m
        val now = System.currentTimeMillis()
        if (bestD > 50 && loc.accuracy < 40) {
            offCount++
            if (offCount >= 3 && now - lastReroute > 15_000) {
                lastReroute = now; offCount = 0
                _state.update { it.copy(rerouting = true) }
                s.destination?.let { compute(it, reroute = true) }
            }
            return
        }
        offCount = 0
        segIdx = best

        val along = r.cum[best] + bestT * (r.cum[best + 1] - r.cum[best])
        val remaining = (r.lengthM - along).coerceAtLeast(0.0)
        if (remaining < 30) {
            _state.update { it.copy(status = Status.ARRIVED, remainingM = 0.0, remainingS = 0.0) }
            if (prefs.voice) voice.say("Vous êtes arrivé à destination.")
            return
        }
        val last = r.points.size - 1
        val idx = r.maneuvers.indexOfFirst { r.cum[it.beginIndex.coerceIn(0, last)] > along + 5 }.let { if (it < 0) r.maneuvers.size - 1 else it }
        val next = r.maneuvers[idx]
        val dist = (r.cum[next.beginIndex.coerceIn(0, last)] - along).coerceAtLeast(0.0)
        val remS = if (r.lengthM > 0) r.timeS * remaining / r.lengthM else 0.0
        _state.update { it.copy(next = next, distToNext = dist, remainingM = remaining, remainingS = remS, rerouting = false) }
        announce(idx, next, dist, loc.speed)
    }

    private fun announce(idx: Int, m: Maneuver, dist: Double, speed: Float) {
        if (!prefs.voice || m.type in 4..6) return       // l'arrivée est annoncée à part
        val v = max(speed.toDouble(), 8.0)                // m/s
        val far = max(250.0, v * 18)                      // ~18 s avant
        val near = max(35.0, v * 4)                       // ~4 s avant
        if (dist <= near && announcedNear != idx) {
            announcedNear = idx; announcedFar = idx
            voice.say(m.pre ?: m.instruction)
        } else if (dist <= far && dist > near * 2 && announcedFar != idx) {
            announcedFar = idx
            val what = (m.alert ?: m.pre ?: m.instruction).replaceFirstChar { it.lowercase() }
            voice.say("Dans ${spokenDistance(dist)}, $what")
        }
    }
}
