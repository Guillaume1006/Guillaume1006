package fr.cockpit.auto.media

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Lecture à voix haute en français. */
class Speaker(ctx: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(ctx.applicationContext, this)

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts.language = Locale.FRANCE
    }

    fun say(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cockpit")
    }

    fun shutdown() {
        tts.shutdown()
    }
}
