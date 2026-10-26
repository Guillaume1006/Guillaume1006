package app.equalizer.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeEngine(var result: EngineResult = EngineResult.Applied) : EqualizerEngine {
    val calls = mutableListOf<Triple<EqState, Boolean, Int>>()
    override fun apply(state: EqState, enabled: Boolean, loudnessDb: Int): EngineResult {
        calls += Triple(state, enabled, loudnessDb)
        return result
    }
}

class EqStateTest {
    @Test fun `starts flat on the five bands seen on S02`() {
        val s = EqState()
        assertEquals(listOf(100, 300, 1000, 3000, 10000), Band.entries.map { it.hz })
        assertTrue(Band.entries.all { s.gain(it) == 0 })
    }

    @Test fun `one step is one dB`() {
        assertEquals(1, EqState().step(Band.HZ300, +1).gain(Band.HZ300))
        assertEquals(-1, EqState().step(Band.HZ300, -1).gain(Band.HZ300))
    }

    @Test fun `stops at plus 8 and minus 8`() {
        var s = EqState()
        repeat(20) { s = s.step(Band.KHZ1, +1) }
        assertEquals(8, s.gain(Band.KHZ1))
        assertFalse(s.canRaise(Band.KHZ1))
        repeat(40) { s = s.step(Band.KHZ1, -1) }
        assertEquals(-8, s.gain(Band.KHZ1))
        assertFalse(s.canLower(Band.KHZ1))
    }

    @Test fun `a step changes one band only`() {
        val s = EqState().step(Band.HZ100, +3)
        assertTrue((Band.entries - Band.HZ100).all { s.gain(it) == 0 })
    }

    @Test fun `rejects a gain outside the range`() {
        assertFailsWith<IllegalArgumentException> { EqState.of(9, 0, 0, 0, 0) }
        assertFailsWith<IllegalArgumentException> { EqState.of(0, 0, 0, -9, 0) }
        assertFailsWith<IllegalArgumentException> { EqState.of(0, 0, 0) }
    }
}

class EqControllerTest {
    private fun controller(
        engine: FakeEngine = FakeEngine(),
        repo: EqRepository = InMemoryEqRepository(),
    ) = EqController(repo, engine)

    @Test fun `pushes the saved setting to the engine at start`() {
        val engine = FakeEngine()
        controller(engine)
        assertEquals(1, engine.calls.size)
    }

    @Test fun `raise and lower reach the engine and are saved`() {
        val engine = FakeEngine()
        val repo = InMemoryEqRepository()
        val c = controller(engine, repo)
        c.raise(Band.HZ100)
        c.raise(Band.HZ100)
        c.lower(Band.KHZ10)
        assertEquals(2, c.state.gain(Band.HZ100))
        assertEquals(-1, c.state.gain(Band.KHZ10))
        assertEquals(c.state, engine.calls.last().first)
        assertEquals(c.state, repo.load().state)
    }

    @Test fun `selecting a profile applies its gains and loudness`() {
        val engine = FakeEngine()
        val c = controller(engine)
        c.select(Presets.BASS_BOOST.id)
        assertEquals(Presets.BASS_BOOST.state, c.state)
        c.select(Presets.HIGH_VOLUME.id)
        assertEquals(6, c.loudnessDb)
        assertEquals(6, engine.calls.last().third)
    }

    @Test fun `moving a band after a profile leaves no active profile`() {
        val c = controller()
        c.select(Presets.VOCAL.id)
        assertEquals(Presets.VOCAL.id, c.activePresetId)
        c.raise(Band.HZ300)
        assertNull(c.activePresetId)
    }

    @Test fun `the Off profile bypasses the effect and another one turns it back on`() {
        val engine = FakeEngine()
        val c = controller(engine)
        c.select(Presets.OFF.id)
        assertFalse(c.enabled)
        assertFalse(engine.calls.last().second)
        c.select(Presets.VOCAL.id)
        assertTrue(c.enabled)
        assertTrue(engine.calls.last().second)
    }

    @Test fun `reset puts every band back to zero and clears loudness`() {
        val c = controller()
        c.select(Presets.HIGH_VOLUME.id)
        c.raise(Band.KHZ3)
        c.reset()
        assertEquals(EqState(), c.state)
        assertEquals(0, c.loudnessDb)
        assertEquals(Presets.ON.id, c.activePresetId)
    }

    @Test fun `says so when the sound path is not available`() {
        val engine = FakeEngine(EngineResult.Unavailable("no route on this phone"))
        val c = controller(engine)
        c.raise(Band.HZ100)
        val status = assertIs<EngineResult.Unavailable>(c.engineStatus)
        assertEquals("no route on this phone", status.reason)
        // The setting is still kept, so it applies once a route works.
        assertEquals(1, c.state.gain(Band.HZ100))
    }

    @Test fun `restores the last setting from storage`() {
        val repo = InMemoryEqRepository()
        controller(repo = repo).apply { select(Presets.BASS_BOOST.id); raise(Band.KHZ10) }
        val second = controller(repo = repo)
        assertEquals(EqState.of(6, 3, 0, 0, 1), second.state)
    }
}
