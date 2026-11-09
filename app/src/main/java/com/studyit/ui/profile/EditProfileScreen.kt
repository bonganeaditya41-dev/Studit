package com.studyit.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.studyit.core.AcademicLevels
import com.studyit.data.model.User
import com.studyit.data.repository.AuthRepository
import com.studyit.data.repository.StorageRepository
import com.studyit.data.repository.UserRepository
import com.studyit.ui.components.Avatar
import com.studyit.ui.components.BackButton
import com.studyit.ui.components.GlassCard
import com.studyit.ui.components.PrimaryButton
import com.studyit.ui.components.StuditDropdown
import com.studyit.ui.components.StuditTextField
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditProfileUiState(
    val loading: Boolean = true,
    val fullName: String = "",
    val bio: String = "",
    val academicClass: String = "",
    val photoUrl: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val storageRepository: StorageRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileUiState())
    val state = _state.asStateFlow()

    private val uid = authRepository.currentUserId.orEmpty()
    private var cached: User? = null

    init {
        viewModelScope.launch {
            val user = userRepository.getUser(uid)
            cached = user
            _state.update {
                it.copy(
                    loading = false,
                    fullName = user?.fullName.orEmpty(),
                    bio = user?.bio.orEmpty(),
                    academicClass = user?.academicClass.orEmpty(),
                    photoUrl = user?.photoUrl.orEmpty()
                )
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(fullName = value, error = null) }
    fun onBioChange(value: String) = _state.update { it.copy(bio = value) }
    fun onClassChange(value: String) = _state.update { it.copy(academicClass = value) }

    fun uploadPhoto(uri: Uri) {
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            storageRepository.uploadProfileImage(uri, uid)
                .onSuccess { url ->
                    _state.update { it.copy(photoUrl = url, saving = false) }
                }
                .onFailure {
                    _state.update { s ->
                        s.copy(saving = false, error = "Photo upload failed. Try a smaller image.")
                    }
                }
        }
    }

    fun save() {
        val current = _state.value
        if (current.fullName.trim().length < 3) {
            _state.update { it.copy(error = "Enter your full name") }
            return
        }
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            userRepository.updateProfile(
                uid = uid,
                fullName = current.fullName,
                bio = current.bio,
                academicClass = current.academicClass,
                photoUrl = current.photoUrl
            ).onSuccess {
                _state.update { it.copy(saving = false, saved = true) }
            }.onFailure { throwable ->
                _state.update {
                    it.copy(saving = false, error = throwable.message ?: "Could not save")
                }
            }
        }
    }
}

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(viewModel::uploadPhoto) }

    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        BackButton(onBack = onBack)
        Spacer(Modifier.height(8.dp))
        Text("Edit profile", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        GlassCard {
            Column {
                Avatar(
                    photoUrl = state.photoUrl,
                    initials = state.fullName.take(1).uppercase().ifBlank { "S" },
                    size = 84.dp
                )
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = { picker.launch("image/*") }) {
                    Text("Change profile picture")
                }
            }
            Spacer(Modifier.height(8.dp))
            StuditTextField(
                value = state.fullName,
                onValueChange = viewModel::onNameChange,
                label = "Full name"
            )
            Spacer(Modifier.height(12.dp))
            StuditTextField(
                value = state.bio,
                onValueChange = viewModel::onBioChange,
                label = "Bio",
                singleLine = false,
                minLines = 3,
                maxLength = 160
            )
            Spacer(Modifier.height(12.dp))
            StuditDropdown(
                label = "Academic level",
                options = AcademicLevels.ALL,
                selected = state.academicClass,
                onSelect = viewModel::onClassChange,
                placeholder = "Select level"
            )
        }

        if (state.error != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                state.error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(18.dp))
        PrimaryButton(
            text = "Save changes",
            loading = state.saving,
            onClick = viewModel::save
        )
        Spacer(Modifier.height(24.dp))
    }
}
