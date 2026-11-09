package com.studyit.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyit.core.isValidEmail
import com.studyit.core.passwordError
import com.studyit.core.usernameError
import com.studyit.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val resetSent: Boolean = false,
    val verificationSent: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun clearError() = _state.update { it.copy(error = null) }

    fun signIn(email: String, password: String) {
        if (!isValidEmail(email)) {
            _state.update { it.copy(error = "Enter a valid email address") }
            return
        }
        if (password.isBlank()) {
            _state.update { it.copy(error = "Enter your password") }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            authRepository.signIn(email, password)
                .onFailure { throwable ->
                    _state.update {
                        it.copy(loading = false, error = friendly(throwable))
                    }
                }
                .onSuccess {
                    _state.update { it.copy(loading = false) }
                }
        }
    }

    fun signUp(
        fullName: String,
        username: String,
        email: String,
        password: String,
        confirmPassword: String,
        academicClass: String
    ) {
        usernameError(username)?.let { message ->
            _state.update { it.copy(error = message) }
            return
        }
        if (fullName.trim().length < 3) {
            _state.update { it.copy(error = "Enter your full name") }
            return
        }
        if (!isValidEmail(email)) {
            _state.update { it.copy(error = "Enter a valid email address") }
            return
        }
        passwordError(password)?.let { message ->
            _state.update { it.copy(error = message) }
            return
        }
        if (password != confirmPassword) {
            _state.update { it.copy(error = "Passwords do not match") }
            return
        }
        if (academicClass.isBlank()) {
            _state.update { it.copy(error = "Select your class or academic level") }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            authRepository.signUp(fullName, username, email, password, academicClass)
                .onFailure { throwable ->
                    _state.update { it.copy(loading = false, error = friendly(throwable)) }
                }
                .onSuccess {
                    _state.update { it.copy(loading = false, verificationSent = true) }
                }
        }
    }

    fun signInWithGoogleToken(idToken: String) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            authRepository.signInWithGoogleToken(idToken)
                .onFailure { throwable ->
                    _state.update { it.copy(loading = false, error = friendly(throwable)) }
                }
                .onSuccess {
                    _state.update { it.copy(loading = false) }
                }
        }
    }

    fun googleFailed(message: String?) {
        _state.update { it.copy(loading = false, error = message ?: "Google sign-in was cancelled") }
    }

    fun resetPassword(email: String) {
        if (!isValidEmail(email)) {
            _state.update { it.copy(error = "Enter a valid email address") }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            authRepository.sendPasswordReset(email)
                .onFailure { throwable ->
                    _state.update { it.copy(loading = false, error = friendly(throwable)) }
                }
                .onSuccess {
                    _state.update { it.copy(loading = false, resetSent = true) }
                }
        }
    }

    private fun friendly(throwable: Throwable): String {
        val raw = throwable.message ?: "Something went wrong"
        return when {
            raw.contains("password is invalid", true) ||
                raw.contains("INVALID_LOGIN_CREDENTIALS", true) ||
                raw.contains("no user record", true) -> "Email or password is incorrect"
            raw.contains("email address is already in use", true) ->
                "An account with this email already exists"
            raw.contains("network", true) -> "Check your internet connection and try again"
            raw.contains("badly formatted", true) -> "Enter a valid email address"
            raw.contains("too many requests", true) -> "Too many attempts. Try again in a while"
            else -> raw
        }
    }
}
