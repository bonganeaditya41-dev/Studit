package com.studyit.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyit.data.model.LeaderboardEntry
import com.studyit.data.model.LeaderboardRange
import com.studyit.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LeaderboardUiState(
    val loading: Boolean = true,
    val range: LeaderboardRange = LeaderboardRange.ALL_TIME,
    val entries: List<LeaderboardEntry> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LeaderboardUiState())
    val state: StateFlow<LeaderboardUiState> = _state.asStateFlow()

    init { load(LeaderboardRange.ALL_TIME) }

    fun setRange(range: LeaderboardRange) = load(range)

    private fun load(range: LeaderboardRange) {
        _state.update { it.copy(range = range, loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                if (range == LeaderboardRange.ALL_TIME) {
                    userRepository.allTimeLeaderboard().mapIndexed { index, user ->
                        LeaderboardEntry(user = user, points = user.totalPoints, position = index + 1)
                    }
                } else {
                    userRepository.windowedLeaderboard(userRepository.startOf(range))
                }
            }.onSuccess { entries ->
                _state.update { it.copy(loading = false, entries = entries) }
            }.onFailure { throwable ->
                _state.update {
                    it.copy(loading = false, error = throwable.message ?: "Could not load leaderboard")
                }
            }
        }
    }
}
