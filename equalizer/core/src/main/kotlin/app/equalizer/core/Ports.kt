package app.equalizer.core

/** What the screens and the controller see of storage. Swapped for Room later. */
interface EqRepository {
    fun load(): Saved
    fun save(saved: Saved)

    data class Saved(
        val state: EqState,
        val enabled: Boolean,
        val activePresetId: String?,
        val loudnessDb: Int,
    )
}

class InMemoryEqRepository(
    private var saved: EqRepository.Saved = EqRepository.Saved(
        state = Presets.ON.state,
        enabled = true,
        activePresetId = Presets.ON.id,
        loudnessDb = 0,
    ),
) : EqRepository {
    override fun load() = saved
    override fun save(saved: EqRepository.Saved) {
        this.saved = saved
    }
}

/** What the audio side must do. The Android class is in the app module. */
interface EqualizerEngine {
    fun apply(state: EqState, enabled: Boolean, loudnessDb: Int): EngineResult
}

sealed interface EngineResult {
    data object Applied : EngineResult
    data class Unavailable(val reason: String) : EngineResult
}
