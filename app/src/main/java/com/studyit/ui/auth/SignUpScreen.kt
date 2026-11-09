package com.studyit.ui.auth

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyit.core.AcademicLevels
import com.studyit.ui.components.BackButton
import com.studyit.ui.components.PrimaryButton
import com.studyit.ui.components.StuditDropdown
import com.studyit.ui.components.StuditTextField

@Composable
fun SignUpScreen(
    onSignedIn: () -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var fullName by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var academicClass by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        BackButton(onBack = onBack)
        Spacer(Modifier.height(8.dp))
        Text("Create your Studit account", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "Your username is permanent and must be unique.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        StuditTextField(
            value = fullName,
            onValueChange = { fullName = it; viewModel.clearError() },
            label = "Full name"
        )
        Spacer(Modifier.height(12.dp))
        StuditTextField(
            value = username,
            onValueChange = { username = it.trim(); viewModel.clearError() },
            label = "Username",
            placeholder = "e.g. aditya_physics",
            supportingText = "3-20 characters, letters, numbers, dots or underscores"
        )
        Spacer(Modifier.height(12.dp))
        StuditTextField(
            value = email,
            onValueChange = { email = it; viewModel.clearError() },
            label = "Email"
        )
        Spacer(Modifier.height(12.dp))
        StuditTextField(
            value = password,
            onValueChange = { password = it; viewModel.clearError() },
            label = "Password",
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(12.dp))
        StuditTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; viewModel.clearError() },
            label = "Confirm password",
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(12.dp))
        StuditDropdown(
            label = "Class / academic level",
            options = AcademicLevels.ALL,
            selected = academicClass,
            onSelect = { academicClass = it },
            placeholder = "Select your level"
        )

        if (state.error != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                state.error.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (state.verificationSent) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Account created. A verification link was sent to your email.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = "Create account",
            loading = state.loading,
            onClick = {
                viewModel.signUp(
                    fullName = fullName,
                    username = username,
                    email = email,
                    password = password,
                    confirmPassword = confirmPassword,
                    academicClass = academicClass
                )
            }
        )
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                "Optional profile picture can be added right after you sign in.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
