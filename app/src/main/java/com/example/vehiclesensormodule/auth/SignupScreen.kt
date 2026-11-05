package com.example.vehiclesensormodule.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.vehiclesensormodule.R
import com.example.vehiclesensormodule.model.VehicleType
import com.example.vehiclesensormodule.network.ApiClient
import com.example.vehiclesensormodule.network.CONNECTION_ERROR_MESSAGE
import com.example.vehiclesensormodule.network.RegisterRequest
import com.example.vehiclesensormodule.network.readableError
import com.example.vehiclesensormodule.ui.components.AppTextField
import com.example.vehiclesensormodule.ui.components.FormGap
import com.example.vehiclesensormodule.ui.components.MessageBanner
import com.example.vehiclesensormodule.ui.components.MessageKind
import com.example.vehiclesensormodule.ui.components.PrimaryButton
import com.example.vehiclesensormodule.ui.components.ScreenBackground
import com.example.vehiclesensormodule.ui.components.VehiclePicker
import com.example.vehiclesensormodule.ui.theme.TextSecondary
import com.example.vehiclesensormodule.ui.theme.VehicleSensorModuleTheme
import kotlinx.coroutines.launch

private const val MIN_PASSWORD_LENGTH = 8

// The username becomes the car screen's web address, so it must be URL-safe
private val USERNAME_PATTERN = Regex("^[A-Za-z0-9_.-]+$")

private const val USERNAME_MAX_LENGTH = 50

@Composable
fun SignupScreen(
    onSignupSuccess: (username: String) -> Unit,
    onBackToLogin: () -> Unit
) {

    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var vehicleType by rememberSaveable { mutableStateOf(VehicleType.Car) }

    // Field errors appear after the first attempt, not while typing for the first time
    var showErrors by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val trimmedUsername = username.trim()
    val trimmedEmail = email.trim()

    val usernameError = when {
        trimmedUsername.isEmpty() -> "Choose a username"
        trimmedUsername.length > USERNAME_MAX_LENGTH -> "Use $USERNAME_MAX_LENGTH characters or fewer"
        !USERNAME_PATTERN.matches(trimmedUsername) -> "Use only letters, numbers, dots, dashes and underscores"
        else -> null
    }

    val emailError =
        if (android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) null
        else "Enter a valid email address"

    val passwordError =
        if (password.length >= MIN_PASSWORD_LENGTH) null
        else "Use at least $MIN_PASSWORD_LENGTH characters"

    val confirmError =
        if (confirmPassword == password) null
        else "Passwords don't match"

    fun submit() {
        showErrors = true

        if (
            usernameError != null ||
            emailError != null ||
            passwordError != null ||
            confirmError != null
        ) {
            return
        }

        scope.launch {

            isLoading = true
            errorMessage = null

            try {

                val response =
                    ApiClient.authService.register(
                        RegisterRequest(
                            username = trimmedUsername,
                            email = trimmedEmail,
                            password = password,
                            vehicleType = vehicleType.id
                        )
                    )

                if (response.isSuccessful) {

                    onSignupSuccess(trimmedUsername)

                } else {

                    errorMessage =
                        response.readableError("Couldn't create your account.")
                }

            } catch (e: Exception) {

                errorMessage = CONNECTION_ERROR_MESSAGE

            } finally {

                isLoading = false
            }
        }
    }

    ScreenBackground {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {

            IconButton(onClick = onBackToLogin) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back to login",
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Create your account",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "You'll use the same username on your car screen, so pick one that's easy to type.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            errorMessage?.let {
                MessageBanner(it, MessageKind.Error)
                Spacer(modifier = Modifier.height(16.dp))
            }

            AppTextField(
                value = username,
                onValueChange = {
                    username = it
                    errorMessage = null
                },
                label = "Username",
                icon = R.drawable.ic_person,
                errorText = usernameError.takeIf { showErrors },
                enabled = !isLoading
            )

            FormGap()

            AppTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                label = "Email",
                icon = R.drawable.ic_email,
                keyboardType = KeyboardType.Email,
                errorText = emailError.takeIf { showErrors },
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "What do you drive?",
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            VehiclePicker(
                selected = vehicleType,
                onSelected = { vehicleType = it },
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            AppTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = "Password",
                icon = R.drawable.ic_lock,
                isPassword = true,
                errorText = passwordError.takeIf { showErrors },
                enabled = !isLoading
            )

            FormGap()

            AppTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    errorMessage = null
                },
                label = "Confirm password",
                icon = R.drawable.ic_lock,
                isPassword = true,
                imeAction = ImeAction.Done,
                onDone = ::submit,
                errorText = confirmError.takeIf { showErrors },
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(20.dp))

            PrimaryButton(
                text = "Create account",
                onClick = ::submit,
                loading = isLoading
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                TextButton(
                    onClick = onBackToLogin,
                    enabled = !isLoading
                ) {
                    Text("Log in")
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun SignupScreenPreview() {
    VehicleSensorModuleTheme {
        SignupScreen(
            onSignupSuccess = {},
            onBackToLogin = {}
        )
    }
}
