package com.example.sensorcollector.utils

import android.media.AudioManager
import android.media.ToneGenerator

object BeepHelper {
    private var toneGenerator: ToneGenerator? = null
    
    fun playBeep() {
        try {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            }
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}

