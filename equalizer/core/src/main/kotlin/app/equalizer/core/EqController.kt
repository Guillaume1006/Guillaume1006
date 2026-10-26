package app.equalizer.core

/**
 * The equalizer's logic, with no Android in it. One instance per process.
 * Every change is saved, then pushed to the engine; the engine's answer is
 * kept in [engineStatus] so the screen can say when the sound path failed.
 */
class EqController(
    private val repository: EqRepository,
    private val engine: EqualizerEngine,
    private val presets: List<Preset> = Presets.BUILTIN,
) {
    var state: EqState = EqState()
        private set
    var enabled: Boolean = true
        private set
    var activePresetId: String? = null
        private set
    var loudnessDb: Int = 0
        private set
    var engineStatus: EngineResult = EngineResult.Applied
        private set

    init {
        val saved = repository.load()
        state = saved.state
        enabled = saved.enabled
        activePresetId = saved.activePresetId
        loudnessDb = saved.loudnessDb
        push()
    }

    fun raise(band: Band) = change { state = state.step(band, +1); activePresetId = null }

    fun lower(band: Band) = change { state = state.step(band, -1); activePresetId = null }

    fun reset() = change { state = state.flat(); activePresetId = Presets.ON.id; loudnessDb = 0 }

    fun setEnabled(on: Boolean) = change { enabled = on }

    fun select(presetId: String) {
        val preset = presets.first { it.id == presetId }
        change {
            state = preset.state
            loudnessDb = preset.loudnessDb
            activePresetId = preset.id
            // The Off profile bypasses the effect; any other profile turns it back on.
            enabled = preset.id != Presets.OFF.id
        }
    }

    private fun change(edit: () -> Unit) {
        edit()
        repository.save(EqRepository.Saved(state, enabled, activePresetId, loudnessDb))
        push()
    }

    private fun push() {
        engineStatus = engine.apply(state, enabled, loudnessDb)
    }
}
