package com.guillaume.motodash.nav

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Radar fixe (OpenStreetMap). [france] : situé en France. */
data class Camera(val id: Long, val lat: Double, val lon: Double, val maxspeed: Int?, val france: Boolean)

/** Radar placé sur l'itinéraire : [along] = distance depuis le départ (m). */
data class RouteCamera(val cam: Camera, val along: Double)

sealed class CameraAlert {
    /** Belgique : radar précis. */
    data class Radar(val id: Long, val distance: Double, val limit: Int?) : CameraAlert()

    /**
     * France : la loi interdit d'indiquer où se trouve le radar. On signale seulement une
     * « zone de danger » de 2 km, dont la position ne permet pas de situer le radar.
     */
    data class DangerZone(val id: Long, val remaining: Double) : CameraAlert()
}

object Cameras {
    const val RADAR_WARN_M = 600.0
    const val ZONE_LENGTH_M = 2000.0
    private const val MAX_FROM_ROUTE_M = 40.0

    /**
     * Début de la zone de danger, avant le radar. Le décalage (300 à 1 700 m) dépend du radar
     * mais pas de sa position visible : la zone ne révèle pas où il se trouve.
     */
    fun zoneStart(rc: RouteCamera): Double = rc.along - (300 + Math.floorMod(rc.cam.id * 2654435761L, 1400L))

    /** Alerte à afficher pour une position [along] sur l'itinéraire, ou null. */
    fun alertAt(cams: List<RouteCamera>, along: Double, alertsFrance: Boolean): CameraAlert? {
        // Zone de danger (France) dans laquelle on se trouve
        if (alertsFrance) {
            cams.firstOrNull { it.cam.france && along >= zoneStart(it) && along <= zoneStart(it) + ZONE_LENGTH_M }?.let {
                return CameraAlert.DangerZone(it.cam.id, zoneStart(it) + ZONE_LENGTH_M - along)
            }
        }
        // Prochain radar précis (Belgique)
        return cams.firstOrNull { !it.cam.france && it.along >= along - 15 && it.along - along <= RADAR_WARN_M }?.let {
            CameraAlert.Radar(it.cam.id, (it.along - along).coerceAtLeast(0.0), it.cam.maxspeed)
        }
    }

    /** Place les radars sur l'itinéraire (ceux à plus de 40 m de la route sont ignorés). */
    fun alongRoute(route: Route, cams: List<Camera>): List<RouteCamera> = cams.mapNotNull { c ->
        val p = LatLon(c.lat, c.lon)
        var bestD = Double.MAX_VALUE; var bestAlong = 0.0
        for (i in 0 until route.points.size - 1) {
            val (t, d) = project(p, route.points[i], route.points[i + 1])
            if (d < bestD) { bestD = d; bestAlong = route.cum[i] + t * (route.cum[i + 1] - route.cum[i]) }
        }
        if (bestD <= MAX_FROM_ROUTE_M) RouteCamera(c, bestAlong) else null
    }.sortedBy { it.along }

    // ───────────── Récupération (Overpass, données OpenStreetMap) ─────────────

    /** Requête : radars à moins de 60 m du tracé ; ceux situés en France sont listés à part. */
    fun query(route: Route): String {
        val keep = ArrayList<LatLon>()
        route.points.forEach { p -> if (keep.isEmpty() || distance(keep.last(), p) >= 400) keep += p }
        route.points.lastOrNull()?.let { keep += it }
        val coords = keep.joinToString(",") { String.format(java.util.Locale.US, "%.5f,%.5f", it.lat, it.lon) }
        return "[out:json][timeout:25];" +
            "area[\"ISO3166-1\"=\"FR\"][admin_level=2]->.fr;" +
            "node[\"highway\"=\"speed_camera\"](around:60,$coords)->.all;" +
            "node.all(area.fr)->.infr;" +
            ".infr out ids;.infr out count;.all out;"
    }

    /** À appeler hors du fil principal. */
    fun fetch(route: Route): List<Camera> {
        val c = URL("https://overpass-api.de/api/interpreter").openConnection() as HttpURLConnection
        c.connectTimeout = 15_000; c.readTimeout = 40_000
        c.requestMethod = "POST"; c.doOutput = true
        c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        c.setRequestProperty("User-Agent", Http.USER_AGENT)
        c.outputStream.use { it.write(("data=" + URLEncoder.encode(query(route), "UTF-8")).toByteArray()) }
        if (c.responseCode !in 200..299) throw IllegalStateException("Radars indisponibles (${c.responseCode})")
        return parse(c.inputStream.bufferedReader().use { it.readText() })
    }

    /** Réponse : d'abord les identifiants des radars en France, puis un « count », puis tous les radars. */
    fun parse(json: String): List<Camera> {
        val els = JSONObject(json).getJSONArray("elements")
        val fr = HashSet<Long>()
        var afterCount = false
        val out = ArrayList<Camera>()
        for (i in 0 until els.length()) {
            val e = els.getJSONObject(i)
            when {
                e.optString("type") == "count" -> afterCount = true
                !afterCount -> fr += e.getLong("id")
                e.has("lat") -> {
                    val tags = e.optJSONObject("tags")
                    val limit = tags?.optString("maxspeed")?.takeWhile { it.isDigit() }?.toIntOrNull()
                    out += Camera(e.getLong("id"), e.getDouble("lat"), e.getDouble("lon"), limit, e.getLong("id") in fr)
                }
            }
        }
        return out.distinctBy { it.id }
    }
}
