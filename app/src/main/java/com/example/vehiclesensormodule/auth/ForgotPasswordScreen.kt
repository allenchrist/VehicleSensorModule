package com.example.vehiclesensormodule.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import com.example.vehiclesensormodule.R
import com.example.vehiclesensormodule.network.ApiClient
import com.example.vehiclesensormodule.network.CONNECTION_ERROR_MESSAGE
import com.example.vehiclesensormodule.network.ForgotPasswordRequest
import com.example.vehiclesensormodule.network.ResetPasswordRequest
import com.example.vehiclesensormodule.network.readableError
import com.example.vehiclesensormodule.ui.components.AppTextField
import com.example.vehiclesensormodule.ui.components.CODE_LENGTH
import com.example.vehiclesensormodule.ui.components.CodeField
import com.example.vehiclesensormodule.ui.components.FormGap
import com.example.vehiclesensormodule.ui.components.MessageBanner
import com.example.vehiclesensormodule.ui.components.MessageKind
import com.example.vehiclesensormodule.ui.components.PrimaryButton
import com.example.vehiclesensormodule.ui.components.ScreenBackground
import com.example.vehiclesensormodule.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MIN_PASSWORD_LENGTH = 8

// Returns an error message to show, or null if the request went through
private suspend fun requestCode(email: String): String? {

    return try {

        val response =
            ApiClient.authService.forgotPassword(ForgotPasswordRequest(email))

        if (response.isSuccessful) null
        else response.readableError("Couldn't send a code.")

    } catch (e: Exception) {

        CONNECTION_ERROR_MESSAGE
    }
}

@Composable
fun ForgotPasswordScreen(
    onPasswordChanged: () -> Unit,
    onBackToLogin: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var codeRequested by rememberSaveable { mutableStateOf(false) }

    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var resendWait by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()

    val trimmedEmail = email.trim()

    LaunchedEffect(resendWait) {
        if (resendWait > 0) {
            delay(1000)
            resendWait -= 1
        }
    }

    fun sendCode() {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            errorMessage = "Enter a valid email address."
            return
        }

        scope.launch {

            isLoading = true
            errorMessage = null
            notice = null

            val error = requestCode(trimmedEmail)

            if (error == null) {
                if (codeRequested) {
                    notice = "If that email has an account, we've sent a new code."
                }
                codeRequested = true
                resendWait = RESEND_WAIT_SECONDS
            } else {
                errorMessage = error
            }

            isLoading = false
        }
    }

    fun changePassword() {
        errorMessage = when {
            code.length != CODE_LENGTH -> "Enter the $CODE_LENGTH-digit code from the email."
            newPassword.length < MIN_PASSWORD_LENGTH -> "Use at least $MIN_PASSWORD_LENGTH characters for your password."
            newPassword != confirmPassword -> "Passwords don't match."
            else -> null
        }

        if (errorMessage != null) {
            return
        }

        scope.launch {

            isLoading = true
            notice = null

            try {

                val response =
                    ApiClient.authService.resetPassword(
                        ResetPasswordRequest(
                            email = trimmedEmail,
                            code = code,
                            newPassword = newPassword
                        )
                    )

                if (response.isSuccessful) {
                    onPasswordChanged()
                } else {
                    errorMessage = response.readableError("Couldn't change your password.")
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

            IconButton(
                onClick = {
                    if (codeRequested) codeRequested = false else onBackToLogin()
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (codeRequested) "Check your email" else "Forgot your password?",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text =
                    if (codeRequested) "If $trimmedEmail has an account, we've sent it a code. It expires in 10 minutes. If you can't see it, check your spam folder."
                    else "Enter your account's email, and we'll send you a code to change your password.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            errorMessage?.let {
                MessageBanner(it, MessageKind.Error)
                Spacer(modifier = Modifier.height(16.dp))
            }

            notice?.let {
                MessageBanner(it, MessageKind.Success)
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (!codeRequested) {

                AppTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = null
                    },
                    label = "Email",
                    icon = R.drawable.ic_email,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                    onDone = ::sendCode,
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(20.dp))

                PrimaryButton(
                    text = "Send code",
                    onClick = ::sendCode,
                    loading = isLoading
                )

            } else {

                CodeField(
                    value = code,
                    onValueChange = {
                        code = it
                        errorMessage = null
                    },
                    enabled = !isLoading
                )

                FormGap()

                AppTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        errorMessage = null
                    },
                    label = "New password",
                    icon = R.drawable.ic_lock,
                    isPassword = true,
                    enabled = !isLoading
                )

                FormGap()

                AppTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = null
                    },
                    label = "Confirm new password",
                    icon = R.drawable.ic_lock,
                    isPassword = true,
                    imeAction = ImeAction.Done,
                    onDone = ::changePassword,
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(20.dp))

                PrimaryButton(
                    text = "Change password",
                    onClick = ::changePassword,
                    loading = isLoading
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = ::sendCode,
                    enabled = !isLoading && resendWait == 0,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        if (resendWait > 0) "Send a new code in $resendWait s"
                        else "Send a new code"
                    )
                }
            }
        }
    }
}
