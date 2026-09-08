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
import com.example.vehiclesensormodule.network.LoginRequest
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (String, String, String) -> Unit,
    onCreateAccount: () -> Unit
) {

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

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

        Spacer(modifier = Modifier.height(12.dp))

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

        Spacer(modifier = Modifier.height(20.dp))

        // --------------------------------------------------
        // LOGIN BUTTON
        // --------------------------------------------------

        Button(
            onClick = {

                if (username.isBlank() || password.isBlank()) {
                    errorMessage =
                        "Username and password are required."
                    return@Button
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

                        if (response.isSuccessful &&
                            response.body() != null
                        ) {

                            val loginResponse =
                                response.body()!!

                            onLoginSuccess(
                                loginResponse.token,
                                loginResponse.user.username,
                                loginResponse.user.email
                            )

                        } else {

                            errorMessage =
                                response.errorBody()
                                    ?.string()
                                    ?: "Invalid username or password."
                        }

                    } catch (e: Exception) {

                        errorMessage =
                            "Unable to connect to the server."

                    } finally {

                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {

            if (isLoading) {

                CircularProgressIndicator()

            } else {

                Text("Login")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --------------------------------------------------
        // CREATE ACCOUNT
        // --------------------------------------------------

        OutlinedButton(
            onClick = onCreateAccount,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("Create Account")
        }

        // --------------------------------------------------
        // ERROR MESSAGE
        // --------------------------------------------------

        if (errorMessage != null) {

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}