package com.studyit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.studyit.data.repository.AuthRepository
import com.studyit.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val uid: String) : AuthState
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState()
        .map { user -> if (user == null) AuthState.SignedOut else AuthState.SignedIn(user.uid) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)

    init {
        viewModelScope.launch {
            authState.collect { state ->
                if (state is AuthState.SignedIn) syncFcmToken(state.uid)
            }
        }
    }

    private suspend fun syncFcmToken(uid: String) {
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            if (token.isNotBlank()) userRepository.updateFcmToken(uid, token)
        }
    }
}
