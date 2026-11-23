package com.guillaume.motodash.nav

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Calcul d'itinéraire moto avec Valhalla (moteur libre d'OpenStreetMap, serveur public gratuit).
 * Profil « motorcycle », consignes en français.
 */
object Valhalla {
    private const val URL_ROUTE = "https://valhalla1.openstreetmap.de/route"

    /** À appeler hors du fil principal. */
    fun route(from: LatLon, to: LatLon, avoidHighways: Boolean, avoidTolls: Boolean, heading: Float? = null): Route {
        val start = JSONObject().put("lat", from.lat).put("lon", from.lon)
        if (heading != null) start.put("heading", heading.toInt()).put("heading_tolerance", 60)
        val body = JSONObject()
            .put("locations", JSONArray().put(start).put(JSONObject().put("lat", to.lat).put("lon", to.lon)))
            .put("costing", "motorcycle")
            .put("costing_options", JSONObject().put("motorcycle", JSONObject()
                .put("use_highways", if (avoidHighways) 0.0 else 1.0)
                .put("use_tolls", if (avoidTolls) 0.0 else 0.5)))
            .put("directions_options", JSONObject().put("units", "kilometers").put("language", "fr-FR"))

        val c = URL(URL_ROUTE).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000; c.readTimeout = 30_000
        c.requestMethod = "POST"; c.doOutput = true
        c.setRequestProperty("Content-Type", "application/json")
        c.setRequestProperty("User-Agent", Http.USER_AGENT)
        c.outputStream.use { it.write(body.toString().toByteArray()) }
        val code = c.responseCode
        val text = (if (code in 200..299) c.inputStream else c.errorStream).bufferedReader().use { it.readText() }
        if (code !in 200..299) {
            val msg = runCatching { JSONObject(text).optString("error") }.getOrNull()
            throw IllegalStateException(if (!msg.isNullOrBlank()) "Itinéraire introuvable : $msg" else "Itinéraire introuvable ($code)")
        }
        return parse(text)
    }

    fun parse(json: String): Route {
        val trip = JSONObject(json).getJSONObject("trip")
        val leg = trip.getJSONArray("legs").getJSONObject(0)
        val points = decodePolyline6(leg.getString("shape"))
        val ms = leg.getJSONArray("maneuvers")
        val maneuvers = (0 until ms.length()).map { i ->
            val m = ms.getJSONObject(i)
            val names = m.optJSONArray("street_names")
            Maneuver(
                type = m.optInt("type"),
                instruction = m.optString("instruction"),
                alert = m.optString("verbal_transition_alert_instruction").ifBlank { null },
                pre = m.optString("verbal_pre_transition_instruction").ifBlank { null },
                street = if (names != null && names.length() > 0) names.getString(0) else null,
                beginIndex = m.optInt("begin_shape_index"),
                exitCount = if (m.has("roundabout_exit_count")) m.optInt("roundabout_exit_count") else null,
            )
        }
        return Route(points, maneuvers, trip.getJSONObject("summary").optDouble("time"))
    }
}

object Http {
    const val USER_AGENT = "MotoDash/1.0 (appli personnelle Android)"

    fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000; c.readTimeout = 15_000
        c.setRequestProperty("User-Agent", USER_AGENT)
        c.setRequestProperty("Accept-Language", "fr")
        if (c.responseCode !in 200..299) throw IllegalStateException("Erreur ${c.responseCode}")
        return c.inputStream.bufferedReader().use { it.readText() }
    }
}
