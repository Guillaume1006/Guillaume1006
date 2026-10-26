package app.equalizer.core

const val LOUDNESS_MAX = 12

data class Preset(
    val id: String,
    val name: String,
    val state: EqState,
    val loudnessDb: Int = 0,
    val builtin: Boolean = true,
) {
    init {
        require(loudnessDb in 0..LOUDNESS_MAX) { "loudness out of range" }
    }
}

/** Same values as replica/schema.sql. Our own starting points, to tune by ear. */
object Presets {
    val ON = Preset("builtin-on", "On", EqState.of(0, 0, 0, 0, 0))
    val OFF = Preset("builtin-off", "Off", EqState.of(0, 0, 0, 0, 0))
    val BASS_BOOST = Preset("builtin-bass-boost", "Bass Boost", EqState.of(6, 3, 0, 0, 0))
    val HIGH_VOLUME = Preset("builtin-high-volume", "High Volume", EqState.of(0, 0, 0, 0, 0), loudnessDb = 6)
    val VOCAL = Preset("builtin-vocal", "Vocal", EqState.of(-2, 1, 3, 3, 0))

    val BUILTIN = listOf(ON, OFF, BASS_BOOST, HIGH_VOLUME, VOCAL)
}
