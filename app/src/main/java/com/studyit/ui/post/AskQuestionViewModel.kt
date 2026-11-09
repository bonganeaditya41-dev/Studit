package com.studyit.ui.post

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyit.core.AcademicLevels
import com.studyit.core.Subjects
import com.studyit.data.local.DraftStore
import com.studyit.data.local.QuestionDraft
import com.studyit.data.model.Community
import com.studyit.data.model.User
import com.studyit.data.repository.AuthRepository
import com.studyit.data.repository.CommunityRepository
import com.studyit.data.repository.PostRepository
import com.studyit.data.repository.StorageRepository
import com.studyit.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AskUiState(
    val title: String = "",
    val description: String = "",
    val subject: String = "",
    val academicClass: String = "",
    val communityId: String = "",
    val communityName: String = "",
    val tags: String = "",
    val imageUri: Uri? = null,
    val communities: List<Community> = emptyList(),
    val publishing: Boolean = false,
    val error: String? = null,
    val publishedPostId: String? = null,
    val draftSaved: Boolean = false
) {
    val canPublish: Boolean
        get() = title.trim().length >= 8 &&
            description.trim().length >= 15 &&
            subject.isNotBlank() &&
            communityId.isNotBlank()
}

@HiltViewModel
class AskQuestionViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val communityRepository: CommunityRepository,
    private val storageRepository: StorageRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val draftStore: DraftStore
) : ViewModel() {

    private val _state = MutableStateFlow(AskUiState())
    val state: StateFlow<AskUiState> = _state.asStateFlow()

    private val uid = authRepository.currentUserId.orEmpty()
    private var cachedUser: User? = null

    init {
        restoreDraft()
        loadCommunities()
        loadUser()
    }

    private fun restoreDraft() {
        val draft = draftStore.load()
        if (draft.isEmpty) return
        _state.update {
            it.copy(
                title = draft.title,
                description = draft.description,
                subject = draft.subject,
                academicClass = draft.academicClass,
                communityId = draft.communityId,
                tags = draft.tags
            )
        }
    }

    private fun loadCommunities() {
        viewModelScope.launch {
            communityRepository.observeCommunities().collect { communities ->
                _state.update { current ->
                    val selected = communities.firstOrNull { it.communityId == current.communityId }
                    current.copy(
                        communities = communities,
                        communityName = selected?.name ?: current.communityName
                    )
                }
            }
        }
    }

    private fun loadUser() {
        viewModelScope.launch {
            cachedUser = userRepository.getUser(uid)
            val level = cachedUser?.academicClass.orEmpty()
            _state.update {
                it.copy(academicClass = if (it.academicClass.isBlank()) level else it.academicClass)
            }
        }
    }

    fun onTitleChange(value: String) = _state.update { it.copy(title = value, error = null) }
    fun onDescriptionChange(value: String) = _state.update { it.copy(description = value, error = null) }
    fun onTagsChange(value: String) = _state.update { it.copy(tags = value) }
    fun onSubjectChange(value: String) = _state.update { it.copy(subject = value, error = null) }
    fun onClassChange(value: String) = _state.update { it.copy(academicClass = value) }
    fun onImagePicked(uri: Uri?) = _state.update { it.copy(imageUri = uri) }
    fun clearImage() = _state.update { it.copy(imageUri = null) }

    fun onCommunityChange(community: Community) = _state.update {
        it.copy(communityId = community.communityId, communityName = community.name, error = null)
    }

    fun saveDraft() {
        val current = _state.value
        draftStore.save(
            QuestionDraft(
                title = current.title,
                description = current.description,
                subject = current.subject,
                academicClass = current.academicClass,
                communityId = current.communityId,
                tags = current.tags
            )
        )
        _state.update { it.copy(draftSaved = true) }
    }

    fun consumeDraftFlag() = _state.update { it.copy(draftSaved = false) }

    fun publish() {
        val current = _state.value
        val author = cachedUser
        if (author == null) {
            _state.update { it.copy(error = "Your profile is still loading. Try again in a moment.") }
            return
        }
        if (current.title.trim().length < 8) {
            _state.update { it.copy(error = "Add a clearer question title (at least 8 characters)") }
            return
        }
        if (current.description.trim().length < 15) {
            _state.update { it.copy(error = "Describe your doubt in a little more detail") }
            return
        }
        if (current.subject.isBlank()) {
            _state.update { it.copy(error = "Choose a subject") }
            return
        }
        if (current.communityId.isBlank()) {
            _state.update { it.copy(error = "Choose a community to post in") }
            return
        }
        if (Subjects.ALL.none { it == current.subject }) {
            _state.update { it.copy(error = "Choose a valid subject") }
            return
        }
        if (current.academicClass.isNotBlank() &&
            AcademicLevels.ALL.none { it == current.academicClass }
        ) {
            _state.update { it.copy(error = "Choose a valid academic level") }
            return
        }

        _state.update { it.copy(publishing = true, error = null) }
        viewModelScope.launch {
            var imageUrl = ""
            current.imageUri?.let { uri ->
                storageRepository.uploadPostImage(uri, uid)
                    .onSuccess { imageUrl = it }
                    .onFailure {
                        _state.update {
                            it.copy(publishing = false, error = "Image upload failed. Try a smaller image.")
                        }
                        return@launch
                    }
            }
            postRepository.createPost(
                author = author,
                communityId = current.communityId,
                communityName = current.communityName,
                title = current.title,
                description = current.description,
                subject = current.subject,
                academicClass = current.academicClass,
                tags = current.tags.split(",").map { it.trim() }.filter { it.isNotBlank() },
                imageUrl = imageUrl
            ).onSuccess { postId ->
                draftStore.clear()
                _state.update { it.copy(publishing = false, publishedPostId = postId) }
            }.onFailure { throwable ->
                _state.update {
                    it.copy(publishing = false, error = throwable.message ?: "Could not publish")
                }
            }
        }
    }
}
