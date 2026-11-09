package com.studyit.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyit.data.model.Answer
import com.studyit.data.model.Post
import com.studyit.data.model.User
import com.studyit.data.repository.AnswerRepository
import com.studyit.data.repository.AuthRepository
import com.studyit.data.repository.PostRepository
import com.studyit.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val loading: Boolean = true,
    val user: User? = null,
    val posts: List<Post> = emptyList(),
    val answers: List<Answer> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val answerRepository: AnswerRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private val myUid = authRepository.currentUserId.orEmpty()
    private var loadedUid: String = ""

    fun bind(uid: String) {
        if (loadedUid == uid) return
        loadedUid = uid
        viewModelScope.launch {
            userRepository.observeUser(uid).collect { user ->
                _state.update {
                    it.copy(
                        loading = false,
                        user = user,
                        error = if (user == null) "This profile is unavailable" else null
                    )
                }
            }
        }
        viewModelScope.launch {
            val posts = postRepository.userPosts(uid)
            _state.update { it.copy(posts = posts) }
        }
    }

    fun loadAnswersFor(postIds: List<String>) {
        // Answers are fetched per post through the detail screen; kept for future use.
    }

    fun isMine(uid: String) = uid == myUid
}
