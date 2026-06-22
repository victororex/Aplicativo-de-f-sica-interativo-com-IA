package com.example.testes.data.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Single-process state bus used to keep ViewModels in sync after any mutation
 * (lesson completed, missions submitted, OCR analyzed, chat sent, etc.).
 *
 * Producers call [emit] from any layer. Consumers (ViewModels) collect once
 * during init and refresh themselves when relevant events fire.
 */
sealed interface AppEvent {
    data object LessonOpened : AppEvent
    data object LessonCompleted : AppEvent
    data object DailyChallengeSubmitted : AppEvent
    data object CampaignStageSubmitted : AppEvent
    data object ChatMessageSent : AppEvent
    data object OcrAnalyzed : AppEvent
    data object VoiceUsed : AppEvent
    data object ProfileUpdated : AppEvent
    data object StudySessionTicked : AppEvent
    data object TrackMissionCompleted : AppEvent
    data object TrackMissionAnswerRecorded : AppEvent
    data object DailyChallengeInstanceSubmitted : AppEvent
}

object AppStateBus {
    private val _events = MutableSharedFlow<AppEvent>(replay = 0, extraBufferCapacity = 16)
    private val fallbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    /** Non-suspending emit usable from Java (LocalBackend) or arbitrary callers. */
    fun emit(event: AppEvent) {
        if (!_events.tryEmit(event)) {
            // Buffer cheia (improvável dado extraBufferCapacity=16) — degrada para coroutine
            fallbackScope.launch { _events.emit(event) }
        }
    }
}

/** Helper para uso em ViewModels: collect dentro do próprio scope. */
fun CoroutineScope.onAppEvent(
    vararg types: Class<out AppEvent>,
    block: suspend (AppEvent) -> Unit
) {
    launch {
        AppStateBus.events.collect { event ->
            if (types.isEmpty() || types.any { it.isInstance(event) }) block(event)
        }
    }
}
