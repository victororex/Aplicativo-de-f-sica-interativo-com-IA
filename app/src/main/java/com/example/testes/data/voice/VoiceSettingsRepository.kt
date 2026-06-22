package com.example.testes.data.voice

import android.content.Context

data class VoiceSettings(
    val remoteVoiceEnabled: Boolean = true,
    val fallbackToLocalTts: Boolean = false
)

class VoiceSettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getSettings(): VoiceSettings {
        return VoiceSettings(
            remoteVoiceEnabled = prefs.getBoolean(KEY_REMOTE_VOICE_ENABLED, true),
            fallbackToLocalTts = prefs.getBoolean(KEY_FALLBACK_LOCAL_TTS, false)
        )
    }

    fun setRemoteVoiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMOTE_VOICE_ENABLED, enabled).apply()
    }

    fun setFallbackToLocalTts(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FALLBACK_LOCAL_TTS, enabled).apply()
    }

    companion object {
        private const val PREFS = "fisica_interativa_voice_settings"
        private const val KEY_REMOTE_VOICE_ENABLED = "remote_voice_enabled"
        private const val KEY_FALLBACK_LOCAL_TTS = "fallback_to_local_tts"
    }
}
