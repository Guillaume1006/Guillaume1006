package app.equalizer.core

const val GAIN_MIN = -8
const val GAIN_MAX = 8

/** The five fixed bands seen on screen S02. */
enum class Band(val hz: Int) {
    HZ100(100),
    HZ300(300),
    KHZ1(1000),
    KHZ3(3000),
    KHZ10(10000),
}

/** Gains in whole dB for every band. Immutable: every change returns a new value. */
data class EqState(val gains: Map<Band, Int> = Band.entries.associateWith { 0 }) {
    init {
        require(gains.keys == Band.entries.toSet()) { "every band needs a gain" }
        require(gains.values.all { it in GAIN_MIN..GAIN_MAX }) { "gain out of range" }
    }

    fun gain(band: Band): Int = gains.getValue(band)

    fun canRaise(band: Band): Boolean = gain(band) < GAIN_MAX

    fun canLower(band: Band): Boolean = gain(band) > GAIN_MIN

    /** One tap = one dB. Stops at the limits instead of failing. */
    fun step(band: Band, delta: Int): EqState =
        EqState(gains + (band to (gain(band) + delta).coerceIn(GAIN_MIN, GAIN_MAX)))

    fun flat(): EqState = EqState()

    companion object {
        fun of(vararg gains: Int): EqState {
            require(gains.size == Band.entries.size) { "need ${Band.entries.size} gains" }
            return EqState(Band.entries.zip(gains.toList()).toMap())
        }
    }
}
