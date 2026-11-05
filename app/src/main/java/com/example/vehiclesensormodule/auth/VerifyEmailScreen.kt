package com.example.vehiclesensormodule.auth

import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.vehiclesensormodule.R
import com.example.vehiclesensormodule.network.ApiClient
import com.example.vehiclesensormodule.network.CONNECTION_ERROR_MESSAGE
import com.example.vehiclesensormodule.network.VerifySignupRequest
import com.example.vehiclesensormodule.network.readableError
import com.example.vehiclesensormodule.ui.components.CODE_LENGTH
import com.example.vehiclesensormodule.ui.components.CodeField
import com.example.vehiclesensormodule.ui.components.MessageBanner
import com.example.vehiclesensormodule.ui.components.MessageKind
import com.example.vehiclesensormodule.ui.components.PrimaryButton
import com.example.vehiclesensormodule.ui.components.ScreenBackground
import com.example.vehiclesensormodule.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// The server allows a new code once a minute
const val RESEND_WAIT_SECONDS = 60

/**
 * The last step of signing up. The account only exists once this succeeds.
 * onResend asks for another code, and returns an error message if it failed.
 */
@Composable
fun VerifyEmailScreen(
    email: String,
    onVerified: () -> Unit,
    onResend: suspend () -> String?,
    onBack: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var resendWait by remember { mutableIntStateOf(RESEND_WAIT_SECONDS) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(resendWait) {
        if (resendWait > 0) {
            delay(1000)
            resendWait -= 1
        }
    }

    fun verify() {
        if (code.length != CODE_LENGTH) {
            errorMessage = "Enter the $CODE_LENGTH-digit code from the email."
            return
        }

        scope.launch {

            isLoading = true
            errorMessage = null
            notice = null

            try {

                val response =
                    ApiClient.authService.verifySignup(
                        VerifySignupRequest(email = email, code = code)
                    )

                if (response.isSuccessful) {
                    onVerified()
                } else {
                    errorMessage = response.readableError("Couldn't check that code.")
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

            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back to your details",
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Check your email",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "We sent a code to $email. It expires in 10 minutes. If you can't see it, check your spam folder.",
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

            CodeField(
                value = code,
                onValueChange = {
                    code = it
                    errorMessage = null
                },
                enabled = !isLoading,
                imeAction = ImeAction.Done,
                onDone = ::verify
            )

            Spacer(modifier = Modifier.height(20.dp))

            PrimaryButton(
                text = "Verify and create account",
                onClick = ::verify,
                loading = isLoading
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = {
                    scope.launch {

                        errorMessage = null
                        notice = null

                        val error = onResend()

                        if (error == null) {
                            notice = "We've sent a new code."
                            resendWait = RESEND_WAIT_SECONDS
                            code = ""
                        } else {
                            errorMessage = error
                        }
                    }
                },
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
