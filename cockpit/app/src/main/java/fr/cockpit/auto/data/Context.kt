package fr.cockpit.auto.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt

fun Context.granted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun Context.hasLocation(): Boolean =
    granted(Manifest.permission.ACCESS_FINE_LOCATION) || granted(Manifest.permission.ACCESS_COARSE_LOCATION)

/** Dernière position connue, sans services Google. */
@SuppressLint("MissingPermission")
fun Context.lastLocation(): Location? {
    if (!hasLocation()) return null
    val lm = getSystemService(LocationManager::class.java) ?: return null
    return lm.getProviders(true)
        .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }
}

/* ---------- Agenda ---------- */

data class NextEvent(val title: String, val beginMs: Long, val location: String?)

/** Prochain rendez-vous dans les 24 h (hors journées entières). */
suspend fun Context.nextEvent(): NextEvent? = withContext(Dispatchers.IO) {
    if (!granted(Manifest.permission.READ_CALENDAR)) return@withContext null
    val now = System.currentTimeMillis()
    val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
        ContentUris.appendId(it, now - 15 * 60_000L)
        ContentUris.appendId(it, now + 24 * 3_600_000L)
    }.build()
    val cols = arrayOf(
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.EVENT_LOCATION,
    )
    val where = "${CalendarContract.Instances.ALL_DAY} = 0 AND ${CalendarContract.Instances.BEGIN} >= ?"
    runCatching {
        contentResolver.query(uri, cols, where, arrayOf((now - 15 * 60_000L).toString()), "${CalendarContract.Instances.BEGIN} ASC")
            ?.use { c ->
                if (!c.moveToFirst()) null
                else NextEvent(c.getString(0)?.takeIf { it.isNotBlank() } ?: "Rendez-vous", c.getLong(1), c.getString(2)?.takeIf { it.isNotBlank() })
            }
    }.getOrNull()
}

/* ---------- Météo (Open-Meteo, gratuit, sans clé) ---------- */

data class Weather(val temp: Int, val label: String, val max: Int, val min: Int, val rainChance: Int?)

suspend fun fetchWeather(lat: Double, lon: Double): Weather? = withContext(Dispatchers.IO) {
    runCatching {
        val url = URL(
            String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
                    "&current=temperature_2m,weather_code" +
                    "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                    "&timezone=auto&forecast_days=1",
                lat, lon,
            )
        )
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 8_000
        conn.readTimeout = 8_000
        val body = try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
        val json = JSONObject(body)
        val current = json.getJSONObject("current")
        val daily = json.getJSONObject("daily")
        Weather(
            temp = current.getDouble("temperature_2m").roundToInt(),
            label = weatherLabel(current.getInt("weather_code")),
            max = daily.getJSONArray("temperature_2m_max").getDouble(0).roundToInt(),
            min = daily.getJSONArray("temperature_2m_min").getDouble(0).roundToInt(),
            rainChance = daily.optJSONArray("precipitation_probability_max")
                ?.takeIf { !it.isNull(0) }?.getInt(0),
        )
    }.getOrNull()
}

private fun weatherLabel(code: Int): String = when (code) {
    0 -> "Ciel dégagé"
    1, 2 -> "Peu nuageux"
    3 -> "Couvert"
    45, 48 -> "Brouillard"
    in 51..57 -> "Bruine"
    in 61..67 -> "Pluie"
    in 71..77 -> "Neige"
    in 80..82 -> "Averses"
    85, 86 -> "Averses de neige"
    in 95..99 -> "Orage"
    else -> "Météo"
}
