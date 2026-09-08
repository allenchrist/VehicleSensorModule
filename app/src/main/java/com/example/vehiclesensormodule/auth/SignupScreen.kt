package com.example.vehiclesensormodule.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.vehiclesensormodule.network.ApiClient
import com.example.vehiclesensormodule.network.RegisterRequest
import kotlinx.coroutines.launch

@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),

        verticalArrangement = Arrangement.Top
    ) {

        Spacer(modifier = Modifier.height(20.dp))

        // --------------------------------------------------
        // TITLE
        // --------------------------------------------------

        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(18.dp))

        // --------------------------------------------------
        // USERNAME
        // --------------------------------------------------

        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                errorMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Username")
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // --------------------------------------------------
        // EMAIL
        // --------------------------------------------------

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Email")
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // --------------------------------------------------
        // PASSWORD
        // --------------------------------------------------

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Password")
            },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // --------------------------------------------------
        // CONFIRM PASSWORD
        // --------------------------------------------------

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                errorMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Confirm Password")
            },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --------------------------------------------------
        // SIGN UP BUTTON
        // --------------------------------------------------

        Button(
            onClick = {

                when {

                    username.isBlank() ||
                            email.isBlank() ||
                            password.isBlank() ||
                            confirmPassword.isBlank() -> {

                        errorMessage = "All fields are required."
                    }

                    !android.util.Patterns.EMAIL_ADDRESS
                        .matcher(email.trim())
                        .matches() -> {

                        errorMessage = "Please enter a valid email address."
                    }

                    password != confirmPassword -> {

                        errorMessage = "Passwords do not match."
                    }

                    else -> {

                        scope.launch {

                            isLoading = true
                            errorMessage = null

                            try {

                                val response =
                                    ApiClient.authService.register(
                                        RegisterRequest(
                                            username = username.trim(),
                                            email = email.trim(),
                                            password = password
                                        )
                                    )

                                if (response.isSuccessful) {

                                    onSignupSuccess()

                                } else {

                                    errorMessage =
                                        response.errorBody()
                                            ?.string()
                                            ?: "Failed to create account."
                                }

                            } catch (e: Exception) {

                                errorMessage =
                                    "Unable to connect to the server."

                            } finally {

                                isLoading = false
                            }
                        }
                    }
                }
            },

            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Text("Sign Up")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --------------------------------------------------
        // ALREADY HAVE ACCOUNT - LOGIN
        // --------------------------------------------------

        OutlinedButton(
            onClick = onBackToLogin,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(
                text = "Already have an account? Login"
            )
        }

        // --------------------------------------------------
        // ERROR MESSAGE
        // --------------------------------------------------

        if (errorMessage != null) {

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}