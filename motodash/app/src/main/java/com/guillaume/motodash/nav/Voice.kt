package com.guillaume.motodash.nav

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Guidage vocal en français. Pendant une annonce, la musique est baissée (et non coupée),
 * puis remonte : comme dans Waze.
 */
class Voice(ctx: Context) : TextToSpeech.OnInitListener {
    private val am = ctx.getSystemService(AudioManager::class.java)
    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attrs).build()
    private val tts = TextToSpeech(ctx.applicationContext, this)
    private var ready = false
    private val pending = ArrayList<String>()

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        tts.language = Locale.FRANCE
        tts.setAudioAttributes(attrs)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) { if (!tts.isSpeaking) am.abandonAudioFocusRequest(focus) }
            @Deprecated("Requis par l'API") override fun onError(id: String?) { am.abandonAudioFocusRequest(focus) }
        })
        ready = true
        pending.forEach { say(it) }
        pending.clear()
    }

    fun say(text: String) {
        if (!ready) { pending += text; return }
        am.requestAudioFocus(focus)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), "nav-${System.nanoTime()}")
    }

    fun stop() { if (ready) tts.stop(); am.abandonAudioFocusRequest(focus) }
    fun shutdown() { stop(); tts.shutdown() }
}
