package com.guillaume.motodash.nav

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLon(val lat: Double, val lon: Double)

/** Un lieu trouvé par la recherche (ou enregistré en favori). */
data class Place(val name: String, val detail: String, val lat: Double, val lon: Double) {
    fun encode() = listOf(name, detail, lat, lon).joinToString("\u001F")

    companion object {
        fun decode(s: String): Place? = s.split('\u001F').takeIf { it.size == 4 }?.let {
            Place(it[0], it[1], it[2].toDoubleOrNull() ?: return null, it[3].toDoubleOrNull() ?: return null)
        }
    }
}

/** Une manœuvre de l'itinéraire (tourner, rond-point…), telle que renvoyée par Valhalla. */
data class Maneuver(
    val type: Int,
    val instruction: String,
    val alert: String?,          // phrase courte, annoncée à l'avance (« Tournez à droite dans … »)
    val pre: String?,            // phrase dite juste avant la manœuvre
    val street: String?,
    val beginIndex: Int,         // index du point de l'itinéraire où commence la manœuvre
    val exitCount: Int?,         // numéro de sortie (rond-point)
)

/** Itinéraire : la ligne (points + distance cumulée) et les manœuvres. */
class Route(val points: List<LatLon>, val maneuvers: List<Maneuver>, val timeS: Double) {
    val cum: DoubleArray = DoubleArray(points.size).also { c ->
        for (i in 1 until points.size) c[i] = c[i - 1] + distance(points[i - 1], points[i])
    }
    val lengthM: Double get() = cum.lastOrNull() ?: 0.0
}

private const val R = 6_371_000.0

fun distance(a: LatLon, b: LatLon): Double {
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLon = Math.toRadians(b.lon - a.lon)
    val h = sin(dLat / 2).let { it * it } +
        cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLon / 2).let { it * it }
    return 2 * R * atan2(sqrt(h), sqrt(1 - h))
}

/**
 * Projection d'un point sur le segment [a, b] (approximation plane, très bonne à cette échelle).
 * Renvoie (fraction 0..1 le long du segment, distance en mètres jusqu'au segment).
 */
fun project(p: LatLon, a: LatLon, b: LatLon): Pair<Double, Double> {
    val k = cos(Math.toRadians(p.lat))
    val ax = a.lon * k; val ay = a.lat
    val bx = b.lon * k; val by = b.lat
    val px = p.lon * k; val py = p.lat
    val dx = bx - ax; val dy = by - ay
    val len2 = dx * dx + dy * dy
    val t = if (len2 == 0.0) 0.0 else (((px - ax) * dx + (py - ay) * dy) / len2).coerceIn(0.0, 1.0)
    val q = LatLon(a.lat + (b.lat - a.lat) * t, a.lon + (b.lon - a.lon) * t)
    return t to distance(p, q)
}

/** Décodage de la ligne encodée de Valhalla (précision 6 décimales). */
fun decodePolyline6(encoded: String): List<LatLon> {
    val out = ArrayList<LatLon>()
    var index = 0; var lat = 0L; var lon = 0L
    while (index < encoded.length) {
        for (pass in 0..1) {
            var result = 0L; var shift = 0
            var b: Int
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f).toLong() shl shift)
                shift += 5
            } while (b >= 0x20)
            val delta = if (result and 1L != 0L) (result shr 1).inv() else result shr 1
            if (pass == 0) lat += delta else lon += delta
        }
        out += LatLon(lat / 1e6, lon / 1e6)
    }
    return out
}

/** « 350 m », « 1,2 km » */
fun formatDistance(m: Double): String = when {
    m < 1000 -> "${((m / 10).toInt() * 10).coerceAtLeast(0)} m"
    m < 10_000 -> String.format(java.util.Locale.FRANCE, "%.1f km", m / 1000)
    else -> "${(m / 1000).toInt()} km"
}

/** Distance dite à voix haute : « 300 mètres », « 1,5 kilomètre ». */
fun spokenDistance(m: Double): String = when {
    m < 1000 -> "${(((m + 25) / 50).toInt() * 50).coerceAtLeast(50)} mètres"
    else -> String.format(java.util.Locale.FRANCE, "%.1f kilomètre", m / 1000).replace(",0 ", " ")
}
