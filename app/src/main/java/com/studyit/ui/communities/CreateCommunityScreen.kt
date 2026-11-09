package com.studyit.ui.communities

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studyit.data.repository.AuthRepository
import com.studyit.data.repository.CommunityRepository
import com.studyit.data.repository.UserRepository
import com.studyit.ui.components.BackButton
import com.studyit.ui.components.GlassCard
import com.studyit.ui.components.PrimaryButton
import com.studyit.ui.components.StuditTextField
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle

data class CreateCommunityUiState(
    val name: String = "",
    val description: String = "",
    val rules: String = "",
    val creating: Boolean = false,
    val error: String? = null,
    val createdId: String? = null
)

@HiltViewModel
class CreateCommunityViewModel @Inject constructor(
    private val communityRepository: CommunityRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CreateCommunityUiState())
    val state = _state.asStateFlow()

    fun onNameChange(value: String) = _state.update { it.copy(name = value, error = null) }
    fun onDescriptionChange(value: String) = _state.update { it.copy(description = value) }
    fun onRulesChange(value: String) = _state.update { it.copy(rules = value) }

    fun create() {
        val current = _state.value
        if (current.name.trim().removePrefix("r/").length < 3) {
            _state.update { it.copy(error = "Give your community a longer name") }
            return
        }
        if (current.description.trim().length < 10) {
            _state.update { it.copy(error = "Describe what this community is about") }
            return
        }
        _state.update { it.copy(creating = true, error = null) }
        viewModelScope.launch {
            val uid = authRepository.currentUserId.orEmpty()
            val user = userRepository.getUser(uid)
            if (user == null) {
                _state.update { it.copy(creating = false, error = "Profile not ready") }
                return@launch
            }
            communityRepository.createCommunity(
                name = current.name,
                description = current.description,
                rules = current.rules.split("\n").filter { it.isNotBlank() },
                creator = user
            ).onSuccess { id ->
                _state.update { it.copy(creating = false, createdId = id) }
            }.onFailure { throwable ->
                _state.update {
                    it.copy(creating = false, error = throwable.message ?: "Could not create community")
                }
            }
        }
    }
}

@Composable
fun CreateCommunityScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: CreateCommunityViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(state.createdId) {
        state.createdId?.let { onCreated(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        BackButton(onBack = onBack)
        Spacer(Modifier.height(8.dp))
        Text("Create a community", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "Communities keep discussions focused — one per subject, exam or class.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        GlassCard {
            StuditTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = "Community name",
                placeholder = "Physics",
                maxLength = 30
            )
            Spacer(Modifier.height(12.dp))
            StuditTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = "Description",
                singleLine = false,
                minLines = 3
            )
            Spacer(Modifier.height(12.dp))
            StuditTextField(
                value = state.rules,
                onValueChange = viewModel::onRulesChange,
                label = "Rules (one per line)",
                singleLine = false,
                minLines = 3
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
            text = "Create community",
            loading = state.creating,
            onClick = viewModel::create
        )
        Spacer(Modifier.height(24.dp))
    }
}
