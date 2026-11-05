package com.example.vehiclesensormodule.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.vehiclesensormodule.R
import com.example.vehiclesensormodule.network.ApiClient
import com.example.vehiclesensormodule.network.CONNECTION_ERROR_MESSAGE
import com.example.vehiclesensormodule.network.LoginRequest
import com.example.vehiclesensormodule.network.readableError
import com.example.vehiclesensormodule.ui.components.AppTextField
import com.example.vehiclesensormodule.ui.components.BrandHeader
import com.example.vehiclesensormodule.ui.components.FormGap
import com.example.vehiclesensormodule.ui.components.MessageBanner
import com.example.vehiclesensormodule.ui.components.MessageKind
import com.example.vehiclesensormodule.ui.components.PrimaryButton
import com.example.vehiclesensormodule.ui.components.ScreenBackground
import com.example.vehiclesensormodule.ui.theme.TextSecondary
import com.example.vehiclesensormodule.ui.theme.VehicleSensorModuleTheme
import kotlinx.coroutines.launch

/**
 * onLoginSuccess finishes signing in and returns an error message if it failed.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: suspend (token: String, username: String) -> String?,
    onCreateAccount: () -> Unit,
    onForgotPassword: () -> Unit = {},
    initialUsername: String = "",
    initialError: String? = null,
    successMessage: String? = null
) {

    var username by rememberSaveable { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf(initialError) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun submit() {
        if (username.isBlank() || password.isBlank()) {
            errorMessage = "Enter your username and password."
            return
        }

        scope.launch {

            isLoading = true
            errorMessage = null

            try {

                val response =
                    ApiClient.authService.login(
                        LoginRequest(
                            username = username.trim(),
                            password = password
                        )
                    )

                val body = response.body()

                errorMessage =
                    if (response.isSuccessful && body != null) {
                        onLoginSuccess(body.token, body.user.username)
                    } else {
                        response.readableError("Invalid username or password.")
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
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center
        ) {

            BrandHeader()

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Welcome back",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Log in to connect your vehicle and pass narrow roads safely.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            val banner = errorMessage

            if (banner != null) {
                MessageBanner(banner, MessageKind.Error)
                Spacer(modifier = Modifier.height(16.dp))
            } else if (successMessage != null) {
                MessageBanner(successMessage, MessageKind.Success)
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
                enabled = !isLoading
            )

            FormGap()

            AppTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = "Password",
                icon = R.drawable.ic_lock,
                isPassword = true,
                imeAction = ImeAction.Done,
                onDone = ::submit,
                enabled = !isLoading
            )

            TextButton(
                onClick = onForgotPassword,
                enabled = !isLoading,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Forgot password?")
            }

            Spacer(modifier = Modifier.height(8.dp))

            PrimaryButton(
                text = "Log in",
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
                    text = "New here?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                TextButton(
                    onClick = onCreateAccount,
                    enabled = !isLoading
                ) {
                    Text("Create an account")
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun LoginScreenPreview() {
    VehicleSensorModuleTheme {
        LoginScreen(
            onLoginSuccess = { _, _ -> null },
            onCreateAccount = {}
        )
    }
}
