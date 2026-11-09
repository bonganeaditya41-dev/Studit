package com.studyit.ui.communities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyit.data.model.Community
import com.studyit.data.model.Post
import com.studyit.data.repository.AuthRepository
import com.studyit.data.repository.CommunityRepository
import com.studyit.data.repository.PostRepository
import com.studyit.data.repository.UserRepository
import com.studyit.data.repository.VoteRepository
import com.studyit.data.model.VoteTargetType
import com.studyit.data.model.VoteType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommunityDetailUiState(
    val loading: Boolean = true,
    val community: Community? = null,
    val posts: List<Post> = emptyList(),
    val joined: Boolean = false,
    val votes: Map<String, VoteType> = emptyMap(),
    val message: String? = null
)

@HiltViewModel
class CommunityDetailViewModel @Inject constructor(
    private val communityRepository: CommunityRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val voteRepository: VoteRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CommunityDetailUiState())
    val state: StateFlow<CommunityDetailUiState> = _state.asStateFlow()

    private val uid = authRepository.currentUserId.orEmpty()
    private var communityId: String = ""

    fun bind(id: String) {
        if (communityId == id) return
        communityId = id
        viewModelScope.launch {
            communityRepository.observeCommunity(id).collect { community ->
                _state.update { it.copy(loading = false, community = community) }
                loadPosts()
            }
        }
        viewModelScope.launch {
            communityRepository.observeMembership(id, uid).collect { joined ->
                _state.update { it.copy(joined = joined) }
            }
        }
        viewModelScope.launch {
            voteRepository.observeMyVotes(uid).collect { votes ->
                _state.update { it.copy(votes = votes) }
            }
        }
    }

    private fun loadPosts() {
        viewModelScope.launch {
            val page = postRepository.feed(
                sort = com.studyit.data.model.FeedSort.LATEST,
                communityId = communityId,
                pageSize = 100,
                lastDocument = null
            )
            _state.update { it.copy(posts = page.posts) }
        }
    }

    fun toggleMembership() {
        val community = _state.value.community ?: return
        viewModelScope.launch {
            val user = userRepository.getUser(uid) ?: return@launch
            val result = if (_state.value.joined) communityRepository.leave(community, user)
            else communityRepository.join(community, user)
            result.onFailure { _state.update { it.copy(message = it.message) } }
        }
    }

    fun vote(post: Post, desired: VoteType) {
        viewModelScope.launch {
            val actor = userRepository.getUser(uid) ?: return@launch
            voteRepository.vote(
                targetType = VoteTargetType.POST,
                targetId = post.postId,
                authorId = post.authorId,
                currentVote = _state.value.votes[post.postId],
                desiredVote = desired,
                actor = actor
            ).onFailure { _state.update { it.copy(message = it.message) } }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
