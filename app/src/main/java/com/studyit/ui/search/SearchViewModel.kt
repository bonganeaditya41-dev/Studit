package com.studyit.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyit.data.local.SearchHistoryStore
import com.studyit.data.model.Community
import com.studyit.data.model.Post
import com.studyit.data.model.User
import com.studyit.data.repository.CommunityRepository
import com.studyit.data.repository.PostRepository
import com.studyit.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SearchTab(val label: String) {
    POSTS("Posts"),
    COMMUNITIES("Communities"),
    USERS("Users"),
    SUBJECTS("Subjects")
}

data class SearchUiState(
    val query: String = "",
    val tab: SearchTab = SearchTab.POSTS,
    val posts: List<Post> = emptyList(),
    val communities: List<Community> = emptyList(),
    val users: List<User> = emptyList(),
    val subjects: List<String> = emptyList(),
    val recent: List<String> = emptyList(),
    val searching: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val communityRepository: CommunityRepository,
    private val userRepository: UserRepository,
    private val historyStore: SearchHistoryStore
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState(recent = historyStore.recent()))
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var job: Job? = null

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        job?.cancel()
        if (value.trim().length < 2) {
            _state.update {
                it.copy(posts = emptyList(), communities = emptyList(), users = emptyList(), subjects = emptyList())
            }
            return
        }
        job = viewModelScope.launch {
            delay(350)
            search(value)
        }
    }

    fun setTab(tab: SearchTab) = _state.update { it.copy(tab = tab) }

    fun submit(query: String) {
        historyStore.add(query)
        _state.update { it.copy(query = query, recent = historyStore.recent()) }
        job?.cancel()
        job = viewModelScope.launch { search(query) }
    }

    fun clearHistory() {
        historyStore.clear()
        _state.update { it.copy(recent = emptyList()) }
    }

    private suspend fun search(raw: String) {
        _state.update { it.copy(searching = true, error = null) }
        val query = raw.trim()
        val posts = postRepository.searchPosts(query)
        val communities = communityRepository.searchCommunities(query)
        val users = userRepository.searchUsers(query)
        val subjects = com.studyit.core.Subjects.ALL.filter { it.contains(query, true) }
        _state.update {
            it.copy(
                searching = false,
                posts = posts,
                communities = communities,
                users = users,
                subjects = subjects
            )
        }
    }
}
