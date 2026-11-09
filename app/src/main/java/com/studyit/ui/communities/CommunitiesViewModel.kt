package com.studyit.ui.communities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyit.data.model.Community
import com.studyit.data.repository.AuthRepository
import com.studyit.data.repository.CommunityRepository
import com.studyit.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommunitiesUiState(
    val loading: Boolean = true,
    val communities: List<Community> = emptyList(),
    val myCommunityIds: List<String> = emptyList(),
    val query: String = "",
    val error: String? = null,
    val message: String? = null
) {
    val visible: List<Community>
        get() = if (query.isBlank()) communities
        else communities.filter { it.name.contains(query, true) || it.description.contains(query, true) }
}

@HiltViewModel
class CommunitiesViewModel @Inject constructor(
    private val communityRepository: CommunityRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CommunitiesUiState())
    val state: StateFlow<CommunitiesUiState> = _state.asStateFlow()

    private val uid = authRepository.currentUserId.orEmpty()

    init {
        viewModelScope.launch {
            communityRepository.observeCommunities().collect { list ->
                _state.update { it.copy(loading = false, communities = list) }
            }
        }
        viewModelScope.launch {
            communityRepository.observeMyCommunities(uid).collect { ids ->
                _state.update { it.copy(myCommunityIds = ids) }
            }
        }
    }

    fun onQueryChange(value: String) = _state.update { it.copy(query = value) }

    fun toggleMembership(community: Community) {
        viewModelScope.launch {
            val user = userRepository.getUser(uid) ?: return@launch
            val joined = _state.value.myCommunityIds.contains(community.communityId)
            val result = if (joined) communityRepository.leave(community, user)
            else communityRepository.join(community, user)
            result.onFailure { throwable ->
                _state.update { it.copy(message = throwable.message ?: "Could not update membership") }
            }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
