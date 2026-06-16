package com.example.testes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.testes.data.api.SessionManager
import com.example.testes.data.local.DailyChallengeRepository
import com.example.testes.data.local.DailyQuestionsRepository
import com.example.testes.data.local.LocalBackend
import com.example.testes.data.local.MissionsRepository
import com.example.testes.data.state.AppEvent
import com.example.testes.data.state.AppStateBus
import com.example.testes.model.DailyChallengeInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DailyChallengeListUiState(
    val instances: List<DailyChallengeInstance> = emptyList(),
    val highestMissionUnlocked: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class DailyChallengeListViewModel(application: Application) : AndroidViewModel(application) {

    private val missionsRepo = MissionsRepository(application)
    private val questionsRepo = DailyQuestionsRepository(application)
    private val dailyRepo = DailyChallengeRepository(questionsRepo)

    private val _state = MutableStateFlow(DailyChallengeListUiState())
    val state: StateFlow<DailyChallengeListUiState> = _state.asStateFlow()

    init {
        reload()
        viewModelScope.launch {
            AppStateBus.events.collect { event ->
                when (event) {
                    AppEvent.TrackMissionCompleted,
                    AppEvent.DailyChallengeInstanceSubmitted -> reload()
                    else -> Unit
                }
            }
        }
    }

    fun reload(today: LocalDate = LocalDate.now()) {
        _state.value = _state.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val highest = runCatching {
                val track = missionsRepo.loadTrack(MissionsRepository.DIMENSIONAL_ANALYSIS_ASSET).getOrThrow()
                LocalBackend.setDimensionalTrackTotal(track.missions.size)
                val completedArr = LocalBackend.completedTrackMissions(SessionManager.accessToken)
                val completedIds = mutableSetOf<String>()
                for (i in 0 until completedArr.length()) completedIds.add(completedArr.getString(i))
                val sorted = track.missions.sortedBy { it.index }
                var top = 0
                for (m in sorted) {
                    val unlocked = m.requirements.all { completedIds.contains(it) }
                    if (unlocked) top = m.index
                }
                top
            }.getOrDefault(0)

            val instances = runCatching {
                dailyRepo.buildForToday(highestMissionUnlocked = highest, today = today)
            }.getOrElse { emptyList() }

            _state.value = DailyChallengeListUiState(
                instances = instances,
                highestMissionUnlocked = highest,
                isLoading = false,
                errorMessage = if (instances.isEmpty() && highest == 0)
                    "Conclua a primeira missão da trilha para liberar desafios diários." else null
            )
        }
    }

    fun getInstance(instanceId: String): DailyChallengeInstance? =
        _state.value.instances.firstOrNull { it.instanceId == instanceId }

    suspend fun loadPicks(instanceId: String): Map<String, Int> =
        dailyRepo.getPicks(instanceId)

    fun recordCompletion(instance: DailyChallengeInstance, picks: Map<String, Int>) {
        viewModelScope.launch {
            val score = instance.questions.count { q -> picks[q.id] == q.correctIndex }
            dailyRepo.recordCompletion(instance, score, picks)
            reload()
        }
    }
}
