package com.studyit.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyit.ui.components.BackButton
import com.studyit.ui.components.PrimaryButton
import com.studyit.ui.components.StuditTextField

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp)
    ) {
        BackButton(onBack = onBack)
        Spacer(Modifier.height(8.dp))
        Text("Reset your password", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "We will email you a secure link to set a new password.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        StuditTextField(
            value = email,
            onValueChange = { email = it; viewModel.clearError() },
            label = "Email"
        )
        if (state.error != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                state.error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (state.resetSent) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Reset link sent. Check your inbox and spam folder.",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = "Send reset link",
            loading = state.loading,
            onClick = { viewModel.resetPassword(email) }
        )
    }
}
