package app.equalizer

import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import app.equalizer.core.Band
import app.equalizer.core.EngineResult
import app.equalizer.core.EqState
import app.equalizer.core.EqualizerEngine
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Route A of milestone 0: an effect on the global output mix (session 0).
 * NOT VERIFIED: this class has never run. Whether session 0 is honoured on a
 * given phone is exactly what the spike must find out; on many recent phones
 * it is ignored. Routes B and C would be other implementations of
 * [EqualizerEngine], so no screen code changes.
 */
class DynamicsProcessingEngine : EqualizerEngine {
    private var processing: DynamicsProcessing? = null
    private var loudness: LoudnessEnhancer? = null

    override fun apply(state: EqState, enabled: Boolean, loudnessDb: Int): EngineResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return EngineResult.Unavailable("Android 9 or newer is required")
        }
        return try {
            val dp = processing ?: create().also { processing = it }
            Band.entries.forEachIndexed { index, band ->
                val edge = upperEdgeHz(band).toFloat()
                dp.setPreEqBandAllChannelsTo(
                    index,
                    DynamicsProcessing.EqBand(true, edge, state.gain(band).toFloat()),
                )
            }
            dp.enabled = enabled
            val le = loudness ?: LoudnessEnhancer(SESSION_OUTPUT_MIX).also { loudness = it }
            le.setTargetGain(loudnessDb * 100) // millibels
            le.enabled = enabled && loudnessDb > 0
            EngineResult.Applied
        } catch (e: RuntimeException) {
            EngineResult.Unavailable(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun create(): DynamicsProcessing {
        val config = DynamicsProcessing.Config.Builder(
            DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
            CHANNELS,
            /* preEqInUse = */ true, Band.entries.size,
            /* mbcInUse = */ false, 0,
            /* postEqInUse = */ false, 0,
            /* limiterInUse = */ true,
        ).build()
        return DynamicsProcessing(PRIORITY, SESSION_OUTPUT_MIX, config)
    }

    /** Each band reaches up to the geometric middle of it and the next one. */
    private fun upperEdgeHz(band: Band): Int {
        val next = Band.entries.getOrNull(band.ordinal + 1) ?: return MAX_HZ
        return sqrt(band.hz.toDouble() * next.hz).roundToInt()
    }

    private companion object {
        const val SESSION_OUTPUT_MIX = 0
        const val CHANNELS = 2
        const val PRIORITY = 0
        const val MAX_HZ = 20_000
    }
}
