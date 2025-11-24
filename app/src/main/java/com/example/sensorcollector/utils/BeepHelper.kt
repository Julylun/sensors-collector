package com.example.sensorcollector.utils

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object BeepHelper {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private var toneGenerator: ToneGenerator? = null

    private suspend fun ensureGenerator(): ToneGenerator = mutex.withLock {
        if (toneGenerator == null) {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        }
        toneGenerator!!
    }

    fun playBeep(durationMs: Int = 500) {
        scope.launch {
            try {
                val generator = ensureGenerator()
                generator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, durationMs)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun release() {
        scope.launch {
            mutex.withLock {
                toneGenerator?.release()
                toneGenerator = null
            }
        }
    }
}



