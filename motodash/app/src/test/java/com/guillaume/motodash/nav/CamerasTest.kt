package com.guillaume.motodash.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CamerasTest {
    private val route = Valhalla.parse(javaClass.getResource("/route_moto.json")!!.readText())
    private fun camAt(index: Int, id: Long, france: Boolean, limit: Int? = 70): Camera {
        val p = route.points[index]
        return Camera(id, p.lat, p.lon, limit, france)
    }

    @Test fun radarsPlacesSurLItineraire() {
        val loin = Camera(9, route.points[300].lat + 0.01, route.points[300].lon, 50, false)   // ~1 km de la route
        val placed = Cameras.alongRoute(route, listOf(camAt(600, 2, false), camAt(200, 1, true), loin))
        assertEquals(listOf(1L, 2L), placed.map { it.cam.id })                                // trié, radar éloigné ignoré
        assertEquals(route.cum[200], placed[0].along, 2.0)
    }

    @Test fun belgiqueRadarPreciseA600m() {
        val cams = Cameras.alongRoute(route, listOf(camAt(600, 2, france = false, limit = 70)))
        val at = cams[0].along
        assertNull(Cameras.alertAt(cams, at - 800, true))                                     // trop loin
        val a = Cameras.alertAt(cams, at - 400, true) as CameraAlert.Radar
        assertEquals(400.0, a.distance, 1.0)
        assertEquals(70, a.limit)
        assertNull(Cameras.alertAt(cams, at + 50, true))                                      // dépassé
    }

    @Test fun franceZoneDeDangerSansPositionDuRadar() {
        val cams = Cameras.alongRoute(route, listOf(camAt(800, 123456, france = true)))
        val rc = cams[0]
        val start = Cameras.zoneStart(rc)
        // Le radar est dans la zone, au moins 300 m après le début et 300 m avant la fin
        assertTrue(rc.along - start >= 300)
        assertTrue(start + Cameras.ZONE_LENGTH_M - rc.along >= 300)
        // Jamais d'alerte « Radar » en France : uniquement une zone de 2 km
        var seen = 0
        var d = start - 1000
        while (d < start + Cameras.ZONE_LENGTH_M + 1000) {
            when (val a = Cameras.alertAt(cams, d, true)) {
                is CameraAlert.Radar -> throw AssertionError("radar précis signalé en France")
                is CameraAlert.DangerZone -> { seen++; assertTrue(a.remaining in 0.0..Cameras.ZONE_LENGTH_M) }
                null -> {}
            }
            d += 10
        }
        assertTrue("zone vue $seen fois", seen in 198..202)                                   // 2 000 m / 10 m
    }

    @Test fun optionFranceDesactiveLesZones() {
        val cams = Cameras.alongRoute(route, listOf(camAt(800, 5, france = true), camAt(400, 6, france = false)))
        val fr = cams.first { it.cam.france }
        assertNull(Cameras.alertAt(cams, Cameras.zoneStart(fr) + 500, alertsFrance = false))
        val be = cams.first { !it.cam.france }
        assertTrue(Cameras.alertAt(cams, be.along - 300, alertsFrance = false) is CameraAlert.Radar)  // la Belgique reste
    }

    @Test fun lectureReponseOverpass() {
        val json = """{"elements":[{"type":"node","id":11},{"type":"count","id":0,"tags":{"nodes":"1"}},
            {"type":"node","id":11,"lat":50.6,"lon":3.06,"tags":{"highway":"speed_camera","maxspeed":"50"}},
            {"type":"node","id":22,"lat":50.72,"lon":3.01,"tags":{"highway":"speed_camera","maxspeed":"70 km/h"}}]}"""
        val cams = Cameras.parse(json)
        assertEquals(2, cams.size)
        assertTrue(cams.first { it.id == 11L }.france)
        assertEquals(false, cams.first { it.id == 22L }.france)
        assertEquals(70, cams.first { it.id == 22L }.maxspeed)
    }
}
