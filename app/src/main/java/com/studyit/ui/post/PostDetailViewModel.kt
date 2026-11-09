package com.studyit.ui.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyit.data.model.Answer
import com.studyit.data.model.AnswerSort
import com.studyit.data.model.Comment
import com.studyit.data.model.Post
import com.studyit.data.model.ReportTargetType
import com.studyit.data.model.User
import com.studyit.data.model.VoteTargetType
import com.studyit.data.model.VoteType
import com.studyit.data.repository.AnswerRepository
import com.studyit.data.repository.AuthRepository
import com.studyit.data.repository.BookmarkRepository
import com.studyit.data.repository.ModerationRepository
import com.studyit.data.repository.PostRepository
import com.studyit.data.repository.UserRepository
import com.studyit.data.repository.VoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PostDetailUiState(
    val loading: Boolean = true,
    val post: Post? = null,
    val answers: List<Answer> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val currentUser: User? = null,
    val myVote: VoteType? = null,
    val answerVotes: Map<String, VoteType> = emptyMap(),
    val bookmarked: Boolean = false,
    val answerSort: AnswerSort = AnswerSort.TOP,
    val sending: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val deleted: Boolean = false
) {
    val sortedAnswers: List<Answer>
        get() = when (answerSort) {
            AnswerSort.TOP -> answers.sortedWith(
                compareByDescending<Answer> { it.isAccepted }
                    .thenByDescending { it.score }
                    .thenByDescending { it.createdAt?.seconds ?: 0L }
            )
            AnswerSort.NEWEST -> answers.sortedWith(
                compareByDescending<Answer> { it.isAccepted }
                    .thenByDescending { it.createdAt?.seconds ?: 0L }
            )
        }

    fun commentsFor(answerId: String): List<Comment> =
        comments.filter { it.answerId == answerId }.sortedBy { it.createdAt?.seconds ?: 0L }
}

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val answerRepository: AnswerRepository,
    private val voteRepository: VoteRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val userRepository: UserRepository,
    private val moderationRepository: ModerationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PostDetailUiState())
    val state: StateFlow<PostDetailUiState> = _state.asStateFlow()

    private val uid = authRepository.currentUserId.orEmpty()
    private var postId: String = ""

    fun bind(id: String) {
        if (postId == id) return
        postId = id
        observePost()
        observeAnswers()
        observeComments()
        observeVote()
        observeBookmark()
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            val user = userRepository.getUser(uid)
            _state.update { it.copy(currentUser = user) }
        }
    }

    private fun observePost() {
        viewModelScope.launch {
            postRepository.observePost(postId).collect { post ->
                _state.update {
                    it.copy(loading = false, post = post, error = if (post == null) "This post is no longer available" else null)
                }
            }
        }
    }

    private fun observeAnswers() {
        viewModelScope.launch {
            answerRepository.observeAnswers(postId).collect { answers ->
                _state.update { it.copy(answers = answers) }
            }
        }
    }

    private fun observeComments() {
        viewModelScope.launch {
            answerRepository.observeComments(postId).collect { comments ->
                _state.update { it.copy(comments = comments) }
            }
        }
    }

    private fun observeVote() {
        viewModelScope.launch {
            voteRepository.observeMyVote(VoteTargetType.POST, postId, uid).collect { vote ->
                _state.update { it.copy(myVote = vote) }
            }
        }
    }

    private fun observeBookmark() {
        viewModelScope.launch {
            bookmarkRepository.isBookmarked(uid, postId).collect { saved ->
                _state.update { it.copy(bookmarked = saved) }
            }
        }
    }

    fun setAnswerSort(sort: AnswerSort) = _state.update { it.copy(answerSort = sort) }

    fun votePost(desired: VoteType) {
        val post = _state.value.post ?: return
        viewModelScope.launch {
            val actor = _state.value.currentUser ?: userRepository.getUser(uid) ?: return@launch
            voteRepository.vote(
                targetType = VoteTargetType.POST,
                targetId = post.postId,
                authorId = post.authorId,
                currentVote = _state.value.myVote,
                desiredVote = desired,
                actor = actor
            ).onFailure { _state.update { s -> s.copy(message = it.message ?: "Vote failed") } }
        }
    }

    fun voteAnswer(answer: Answer, desired: VoteType) {
        viewModelScope.launch {
            val actor = _state.value.currentUser ?: userRepository.getUser(uid) ?: return@launch
            voteRepository.vote(
                targetType = VoteTargetType.ANSWER,
                targetId = answer.answerId,
                authorId = answer.authorId,
                currentVote = _state.value.answerVotes[answer.answerId],
                desiredVote = desired,
                actor = actor
            ).onSuccess {
                _state.update { s ->
                    val updated = s.answerVotes.toMutableMap()
                    val newVote = if (s.answerVotes[answer.answerId] == desired) null else desired
                    if (newVote == null) updated.remove(answer.answerId)
                    else updated[answer.answerId] = newVote
                    s.copy(answerVotes = updated)
                }
            }.onFailure { _state.update { s -> s.copy(message = it.message ?: "Vote failed") } }
        }
    }

    fun submitAnswer(content: String) {
        val post = _state.value.post ?: return
        if (content.trim().length < 5) {
            _state.update { it.copy(message = "Write a little more before posting") }
            return
        }
        _state.update { it.copy(sending = true) }
        viewModelScope.launch {
            val author = _state.value.currentUser ?: userRepository.getUser(uid)
            if (author == null) {
                _state.update { it.copy(sending = false, message = "Your profile is still loading") }
                return@launch
            }
            answerRepository.addAnswer(post, author, content)
                .onSuccess { _state.update { it.copy(sending = false, message = "Answer posted") } }
                .onFailure { _state.update { s -> s.copy(sending = false, message = it.message ?: "Could not post answer") } }
        }
    }

    fun acceptAnswer(answer: Answer) {
        val post = _state.value.post ?: return
        viewModelScope.launch {
            val actor = _state.value.currentUser ?: userRepository.getUser(uid) ?: return@launch
            answerRepository.acceptAnswer(post, answer, actor)
                .onSuccess { _state.update { it.copy(message = "Answer accepted") } }
                .onFailure { _state.update { s -> s.copy(message = it.message ?: "Could not accept") } }
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            bookmarkRepository.toggle(uid, postId)
                .onFailure { _state.update { s -> s.copy(message = it.message ?: "Could not save") } }
        }
    }

    fun addComment(answerId: String, parentCommentId: String, content: String) {
        val post = _state.value.post ?: return
        if (content.trim().length < 2) return
        viewModelScope.launch {
            val author = _state.value.currentUser ?: userRepository.getUser(uid) ?: return@launch
            answerRepository.addComment(post, answerId, parentCommentId, author, content)
                .onFailure { _state.update { s -> s.copy(message = it.message ?: "Could not reply") } }
        }
    }

    fun deleteComment(comment: Comment) {
        viewModelScope.launch {
            answerRepository.deleteComment(comment)
        }
    }

    fun deleteAnswer(answer: Answer) {
        viewModelScope.launch {
            answerRepository.deleteAnswer(answer)
                .onSuccess { _state.update { it.copy(message = "Answer deleted") } }
                .onFailure { _state.update { s -> s.copy(message = it.message ?: "Could not delete") } }
        }
    }

    fun deletePost() {
        viewModelScope.launch {
            postRepository.deletePost(postId)
                .onSuccess { _state.update { it.copy(deleted = true) } }
                .onFailure { _state.update { s -> s.copy(message = it.message ?: "Could not delete") } }
        }
    }

    fun report(targetId: String, targetType: ReportTargetType, reason: String) {
        viewModelScope.launch {
            moderationRepository.report(uid, targetId, targetType, reason)
                .onSuccess { _state.update { it.copy(message = "Report sent to moderators") } }
                .onFailure { _state.update { s -> s.copy(message = it.message ?: "Could not send report") } }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
