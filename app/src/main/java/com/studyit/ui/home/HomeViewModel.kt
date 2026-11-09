package com.studyit.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentSnapshot
import com.studyit.core.Defaults
import com.studyit.data.model.FeedSort
import com.studyit.data.model.Post
import com.studyit.data.model.VoteTargetType
import com.studyit.data.model.VoteType
import com.studyit.data.repository.AuthRepository
import com.studyit.data.repository.BookmarkRepository
import com.studyit.data.repository.PostRepository
import com.studyit.data.repository.UserRepository
import com.studyit.data.repository.VoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val posts: List<Post> = emptyList(),
    val sort: FeedSort = FeedSort.LATEST,
    val votes: Map<String, VoteType> = emptyMap(),
    val bookmarks: Set<String> = emptySet(),
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val voteRepository: VoteRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val uid: String = authRepository.currentUserId.orEmpty()
    private var lastDocument: DocumentSnapshot? = null
    private var endReached = false
    private var loadJob: Job? = null

    init {
        observeVotes()
        observeBookmarks()
        refresh()
    }

    private fun observeVotes() {
        viewModelScope.launch {
            voteRepository.observeMyVotes(uid).collect { votes ->
                _state.update { it.copy(votes = votes) }
            }
        }
    }

    private fun observeBookmarks() {
        viewModelScope.launch {
            bookmarkRepository.observeBookmarkedPostIds(uid).collect { ids ->
                _state.update { it.copy(bookmarks = ids.toSet()) }
            }
        }
    }

    fun setSort(sort: FeedSort) {
        if (_state.value.sort == sort) return
        _state.update { it.copy(sort = sort, loading = true, posts = emptyList()) }
        lastDocument = null
        endReached = false
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        lastDocument = null
        endReached = false
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                postRepository.feed(
                    sort = _state.value.sort,
                    communityId = null,
                    pageSize = Defaults.FEED_PAGE_SIZE,
                    lastDocument = null
                )
            }.onSuccess { page ->
                lastDocument = page.lastDocument
                endReached = page.endReached
                _state.update {
                    it.copy(loading = false, posts = page.posts, error = null)
                }
            }.onFailure { throwable ->
                _state.update {
                    it.copy(loading = false, error = throwable.message ?: "Could not load the feed")
                }
            }
        }
    }

    fun loadMore() {
        if (endReached || _state.value.loadingMore || _state.value.loading) return
        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true) }
            runCatching {
                postRepository.feed(
                    sort = _state.value.sort,
                    communityId = null,
                    pageSize = Defaults.FEED_PAGE_SIZE,
                    lastDocument = lastDocument
                )
            }.onSuccess { page ->
                lastDocument = page.lastDocument
                endReached = page.endReached
                _state.update {
                    it.copy(
                        loadingMore = false,
                        posts = it.posts + page.posts.filterNot { post ->
                            it.posts.any { existing -> existing.postId == post.postId }
                        }
                    )
                }
            }.onFailure {
                _state.update { it.copy(loadingMore = false, message = "Could not load more posts") }
            }
        }
    }

    fun vote(post: Post, desired: VoteType) {
        viewModelScope.launch {
            val actor = userRepository.getUser(uid) ?: return@launch
            val current = _state.value.votes[post.postId]
            voteRepository.vote(
                targetType = VoteTargetType.POST,
                targetId = post.postId,
                authorId = post.authorId,
                currentVote = current,
                desiredVote = desired,
                actor = actor
            ).onFailure { throwable ->
                _state.update { it.copy(message = throwable.message ?: "Vote failed") }
            }
        }
    }

    fun toggleBookmark(post: Post) {
        viewModelScope.launch {
            bookmarkRepository.toggle(uid, post.postId)
                .onFailure { throwable ->
                    _state.update { it.copy(message = throwable.message ?: "Could not save this post") }
                }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
