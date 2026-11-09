package com.studyit.ui.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyit.data.model.Post
import com.studyit.data.repository.AuthRepository
import com.studyit.data.repository.BookmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookmarksUiState(
    val loading: Boolean = true,
    val posts: List<Post> = emptyList(),
    val message: String? = null
)

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BookmarksUiState())
    val state: StateFlow<BookmarksUiState> = _state.asStateFlow()

    private val uid = authRepository.currentUserId.orEmpty()

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val posts = bookmarkRepository.bookmarkedPosts(uid)
            _state.update { it.copy(loading = false, posts = posts) }
        }
    }

    fun remove(post: Post) {
        viewModelScope.launch {
            bookmarkRepository.toggle(uid, post.postId).onSuccess { refresh() }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
