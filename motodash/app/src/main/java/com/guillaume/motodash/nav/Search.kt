package com.guillaume.motodash.nav

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Recherche d'adresses (OpenStreetMap) : Photon, et Nominatim en secours.
 * À appeler hors du fil principal.
 */
object Search {
    fun search(query: String, near: LatLon?): List<Place> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        return runCatching { photon(q, near) }.getOrNull()?.takeIf { it.isNotEmpty() }
            ?: nominatim(q, near)
    }

    private fun photon(q: String, near: LatLon?): List<Place> {
        val bias = near?.let { "&lat=${it.lat}&lon=${it.lon}" } ?: ""
        val json = JSONObject(Http.get("https://photon.komoot.io/api/?q=$q&lang=fr&limit=8$bias"))
        val features = json.getJSONArray("features")
        return (0 until features.length()).mapNotNull { i ->
            val f = features.getJSONObject(i)
            val c = f.getJSONObject("geometry").getJSONArray("coordinates")
            val p = f.getJSONObject("properties")
            val street = listOfNotNull(p.optString("housenumber").ifBlank { null }, p.optString("street").ifBlank { null }).joinToString(" ")
            val name = p.optString("name").ifBlank { street.ifBlank { null } } ?: return@mapNotNull null
            val detail = listOf(if (street != name) street else "", p.optString("postcode"), p.optString("city"))
                .filter { it.isNotBlank() }.joinToString(", ")
            Place(name, detail, c.getDouble(1), c.getDouble(0))
        }
    }

    private fun nominatim(q: String, near: LatLon?): List<Place> {
        val view = near?.let { "&viewbox=${it.lon - 0.5},${it.lat + 0.5},${it.lon + 0.5},${it.lat - 0.5}" } ?: ""
        val arr = JSONArray(Http.get("https://nominatim.openstreetmap.org/search?q=$q&format=jsonv2&limit=8&accept-language=fr$view"))
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val full = o.optString("display_name")
            val name = o.optString("name").ifBlank { full.substringBefore(",") }
            Place(name, full.substringAfter(", ", "").take(80), o.getString("lat").toDouble(), o.getString("lon").toDouble())
        }
    }
}
