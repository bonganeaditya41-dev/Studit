package com.studyit.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.studyit.data.model.User
import com.studyit.data.repository.AuthRepository
import com.studyit.data.repository.UserRepository
import com.studyit.ui.components.BackButton
import com.studyit.ui.components.GlassCard
import com.studyit.ui.components.PrimaryButton
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val user: User? = null,
    val emailVerified: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state = _state.asStateFlow()

    private val uid = authRepository.currentUserId.orEmpty()

    init {
        viewModelScope.launch {
            val user = userRepository.getUser(uid)
            _state.update {
                it.copy(user = user, emailVerified = authRepository.currentFirebaseUser()?.isEmailVerified == true)
            }
        }
    }

    fun sendVerification() {
        viewModelScope.launch {
            authRepository.sendEmailVerification()
                .onSuccess { _state.update { s -> s.copy(message = "Verification email sent") } }
                .onFailure { _state.update { s -> s.copy(message = it.message) } }
        }
    }

    fun setProfilePublic(isPublic: Boolean) {
        viewModelScope.launch {
            userRepository.updatePrivacy(uid, isPublic)
            _state.update { it.copy(user = it.user?.copy(isProfilePublic = isPublic)) }
        }
    }

    fun signOut() = authRepository.signOut()

    fun consumeMessage() = _state.update { it.copy(message = null) }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmSignOut by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) { viewModel.consumeMessage() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        BackButton(onBack = onBack)
        Spacer(Modifier.height(8.dp))
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        GlassCard {
            Text("Account", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Email: " + (state.user?.email ?: "-"),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Username: @" + (state.user?.username ?: "-") + " (permanent)",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (state.emailVerified) "Email verified" else "Email not verified",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (!state.emailVerified) {
                TextButton(onClick = viewModel::sendVerification) {
                    Text("Send verification email")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Public profile", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Let other students see your points and questions",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.user?.isProfilePublic ?: true,
                    onCheckedChange = viewModel::setProfilePublic
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        GlassCard {
            Text("Community guidelines", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Be respectful, cite sources where possible, and never post exam papers or " +
                    "content you do not own. Reported content is reviewed by community moderators.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(20.dp))
        PrimaryButton(text = "Sign out", onClick = { confirmSignOut = true })
        Spacer(Modifier.height(24.dp))
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out of Studit?") },
            text = { Text("You can sign back in any time with your email and password.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    viewModel.signOut()
                }) { Text("Sign out") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") }
            }
        )
    }
}
