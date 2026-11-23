package com.guillaume.motodash.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Vrai itinéraire moto Valhalla (Bas-Warneton → Lille), 16 km, 31 manœuvres. */
class RouteTest {
    private val route = Valhalla.parse(javaClass.getResource("/route_moto.json")!!.readText())

    @Test fun lectureItineraire() {
        assertEquals(31, route.maneuvers.size)
        assertEquals(1147, route.points.size)                       // dernière manœuvre : index 1146
        assertEquals(16_002.0, route.lengthM, 300.0)                 // Valhalla annonce 16,002 km
        assertEquals("Tournez à droite dans Route de Linselles.", route.maneuvers[1].alert)
        assertEquals(1, route.maneuvers[3].exitCount)                // rond-point, 1re sortie
    }

    @Test fun auDepartLaProchaineManoeuvreEstLaPremiereVraie() {
        val pr = route.progress(route.points[0], 0)!!
        assertEquals(1, pr.nextIdx)                                   // « Tournez à droite… »
        assertEquals(699.0, pr.distToNext, 40.0)                      // le 1er tronçon fait 699 m
        assertTrue(pr.offRouteM < 1)
    }

    @Test fun enAvancantLesManoeuvresSeSuiventDansLOrdre() {
        var seg = 0; var lastIdx = 0; var lastRemaining = Double.MAX_VALUE
        for (i in 0 until route.points.size step 3) {
            val pr = route.progress(route.points[i], seg)!!
            seg = pr.seg
            assertTrue("manœuvre qui recule au point $i", pr.nextIdx >= lastIdx)
            assertTrue("distance restante qui augmente au point $i", pr.remaining <= lastRemaining + 1)
            lastIdx = pr.nextIdx; lastRemaining = pr.remaining
        }
        assertEquals(route.maneuvers.size - 1, lastIdx)               // on finit sur « Vous êtes arrivé »
        assertTrue(lastRemaining < 60)
    }

    @Test fun horsDeLaRouteEstDetecte() {
        val p = route.points[20]
        val loin = LatLon(p.lat + 0.002, p.lon)                       // ~220 m au nord
        val pr = route.progress(loin, 20)!!
        assertTrue(pr.offRouteM > 150)
    }

    @Test fun distances() {
        assertEquals("350 m", formatDistance(354.0))
        assertEquals("1,2 km", formatDistance(1234.0))
        assertEquals("300 mètres", spokenDistance(310.0))
    }
}
