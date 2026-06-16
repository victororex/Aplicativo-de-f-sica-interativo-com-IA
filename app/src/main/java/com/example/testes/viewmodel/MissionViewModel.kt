package com.example.testes.viewmodel

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.testes.data.api.LearningApiClient
import com.example.testes.data.api.SessionManager
import com.example.testes.data.local.LocalBackend
import com.example.testes.data.local.MissionsRepository
import com.example.testes.model.CampaignNode
import com.example.testes.model.MissionDetail
import com.example.testes.model.MissionNode
import com.example.testes.model.MissionStatus
import com.example.testes.model.MissionTrack
import com.example.testes.model.SubjectPlanet
import com.example.testes.ui.theme.PlanetAmber
import com.example.testes.ui.theme.PlanetBlue
import com.example.testes.ui.theme.PlanetCoral
import com.example.testes.ui.theme.PlanetMint
import com.example.testes.ui.theme.PlanetPurple
import com.example.testes.ui.theme.PlanetTeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MissionUiState(
    val planets: List<SubjectPlanet> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class MissionViewModel(application: Application) : AndroidViewModel(application) {

    private val api = LearningApiClient()
    private val missionsRepo = MissionsRepository(application)

    private val _state = MutableStateFlow(MissionUiState())
    val state: StateFlow<MissionUiState> = _state.asStateFlow()

    private var dimensionalTrack: MissionTrack? = null
    private val completedMissionIds = mutableSetOf<String>()

    init { reload() }

    fun reload() {
        _state.value = _state.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            dimensionalTrack = missionsRepo
                .loadTrack(MissionsRepository.DIMENSIONAL_ANALYSIS_ASSET)
                .getOrNull()
            dimensionalTrack?.let { LocalBackend.setDimensionalTrackTotal(it.missions.size) }

            hydrateCompletedFromStorage()

            api.getCampaign()
                .onSuccess { nodes ->
                    val planets = groupIntoPlanets(nodes).overrideDimensional()
                    _state.value = MissionUiState(planets = planets, isLoading = false)
                }
                .onFailure {
                    val planets = listOfNotNull(buildDimensionalPlanet())
                    _state.value = MissionUiState(
                        planets = planets,
                        isLoading = false,
                        errorMessage = if (planets.isEmpty()) "Não consegui carregar a galáxia." else null
                    )
                }
        }
    }

    fun getPlanet(id: String): SubjectPlanet? = _state.value.planets.firstOrNull { it.id == id }

    /** Re-hidrata o set de missões concluídas da storage e rebuild só do planeta dimensional. Sem API. */
    fun refreshFromStorage() {
        viewModelScope.launch {
            hydrateCompletedFromStorage()
            val current = _state.value.planets
            val updated = current.map { planet ->
                if (planet.matchesDimensional()) buildDimensionalPlanet(planet.color) ?: planet
                else planet
            }
            if (updated.none { it.matchesDimensional() }) {
                val added = buildDimensionalPlanet()
                _state.value = _state.value.copy(planets = if (added != null) updated + added else updated)
            } else {
                _state.value = _state.value.copy(planets = updated)
            }
        }
    }

    fun getMissionDetail(missionId: String): MissionDetail? =
        dimensionalTrack?.missions?.firstOrNull { it.id == missionId }

    fun markMissionCompleted(missionId: String) {
        if (!completedMissionIds.add(missionId)) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                LocalBackend.markTrackMissionCompleted(SessionManager.accessToken, missionId)
            }
        }
        dimensionalTrack?.let {
            val current = _state.value.planets
            val updated = current.map { planet ->
                if (planet.matchesDimensional()) buildDimensionalPlanet(planet.color) ?: planet
                else planet
            }
            _state.value = _state.value.copy(planets = updated)
        }
    }

    /** Maior índice de missão atualmente liberado (CURRENT ou COMPLETED) na trilha. 0 se nada. */
    fun getHighestMissionUnlocked(subjectId: String): Int {
        val track = dimensionalTrack ?: return 0
        if (subjectId != MissionsRepository.DIMENSIONAL_SUBJECT_ID) return 0
        val sorted = track.missions.sortedBy { it.index }
        var highest = 0
        for (m in sorted) {
            val unlocked = m.requirements.all { completedMissionIds.contains(it) }
            if (unlocked) highest = m.index
        }
        return highest
    }

    private suspend fun hydrateCompletedFromStorage() {
        withContext(Dispatchers.IO) {
            runCatching {
                val arr = LocalBackend.completedTrackMissions(SessionManager.accessToken)
                completedMissionIds.clear()
                for (i in 0 until arr.length()) {
                    completedMissionIds.add(arr.getString(i))
                }
            }
        }
    }

    private fun List<SubjectPlanet>.overrideDimensional(): List<SubjectPlanet> {
        val track = dimensionalTrack ?: return this
        val replacement = buildDimensionalPlanet() ?: return this
        val existingIndex = indexOfFirst { it.matchesDimensional() }
        return if (existingIndex >= 0) {
            toMutableList().apply {
                this[existingIndex] = replacement.copy(color = this[existingIndex].color)
            }
        } else {
            this + replacement
        }
    }

    private fun SubjectPlanet.matchesDimensional(): Boolean {
        val track = dimensionalTrack ?: return false
        return id.equals(track.subjectId, ignoreCase = true) ||
            name.equals(track.name, ignoreCase = true) ||
            name.equals(MissionsRepository.DIMENSIONAL_NAME, ignoreCase = true)
    }

    private fun buildDimensionalPlanet(color: Color = PlanetBlue): SubjectPlanet? {
        val track = dimensionalTrack ?: return null
        val sorted = track.missions.sortedBy { it.index }
        val firstUnlocked = sorted.firstOrNull { isUnlocked(it) && !completedMissionIds.contains(it.id) }
        val missions = sorted.mapIndexed { i, m ->
            val status = when {
                completedMissionIds.contains(m.id) -> MissionStatus.COMPLETED
                m.id == firstUnlocked?.id -> MissionStatus.CURRENT
                else -> MissionStatus.LOCKED
            }
            MissionNode(
                id = m.id,
                title = m.title,
                description = m.subtitle.ifBlank { m.title },
                status = status,
                order = i,
                subjectId = track.subjectId,
                stageLabel = "Missão ${m.index}"
            )
        }
        val total = missions.size.takeIf { it > 0 } ?: 1
        val progress = completedMissionIds.count { id -> sorted.any { it.id == id } }.toFloat() / total
        return SubjectPlanet(
            id = track.subjectId,
            name = track.name,
            color = color,
            progress = progress.coerceIn(0f, 1f),
            missions = missions
        )
    }

    private fun isUnlocked(m: MissionDetail): Boolean =
        m.requirements.all { req -> completedMissionIds.contains(req) }

    private fun groupIntoPlanets(nodes: List<CampaignNode>): List<SubjectPlanet> {
        val grouped = nodes.groupBy { it.subjectId.ifBlank { it.subjectName } }
        return grouped.entries.mapIndexed { index, (subjectKey, subjectNodes) ->
            val sorted = subjectNodes.sortedBy { it.stageLabel }
            val firstCurrentId = sorted.firstOrNull { it.isUnlocked && it.progress < 1f }?.id
            val missions = sorted.mapIndexed { i, node ->
                val status = when {
                    node.progress >= 1f -> MissionStatus.COMPLETED
                    node.id == firstCurrentId -> MissionStatus.CURRENT
                    else -> MissionStatus.LOCKED
                }
                MissionNode(
                    id = node.id,
                    title = node.title,
                    description = node.description,
                    status = status,
                    order = i,
                    subjectId = node.subjectId,
                    stageLabel = node.stageLabel
                )
            }
            val overall = if (missions.isEmpty()) 0f else sorted.sumOf { it.progress.toDouble() }.toFloat() / sorted.size
            SubjectPlanet(
                id = subjectKey,
                name = subjectNodes.firstOrNull()?.subjectName?.takeIf { it.isNotBlank() } ?: subjectKey,
                color = palette[index % palette.size],
                progress = overall.coerceIn(0f, 1f),
                missions = missions
            )
        }
    }

    companion object {
        private val palette: List<Color> = listOf(
            PlanetBlue, PlanetPurple, PlanetTeal, PlanetAmber, PlanetCoral, PlanetMint
        )
    }
}
