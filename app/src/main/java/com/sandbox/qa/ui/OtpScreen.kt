package com.sandbox.qa.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sandbox.qa.data.SandboxContract

@Composable
fun OtpScreen(
    phone: String,
    onSuccess: () -> Unit,
    viewModel: OtpViewModel = viewModel(factory = OtpViewModel.factory(phone)),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.signedIn) {
        if (uiState.signedIn) {
            viewModel.onSignedInHandled()
            onSuccess()
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Enter the code from SMS",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.testTag(AuthTags.OTP_TITLE),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Sandbox code: ${SandboxContract.VALID_OTP}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(AuthTags.OTP_HINT),
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = uiState.code,
            onValueChange = viewModel::onCodeChange,
            label = { Text("Confirmation code") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(AuthTags.OTP_INPUT),
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = viewModel::confirm,
            enabled = !uiState.loading,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(AuthTags.OTP_CONFIRM),
        ) {
            Text("Confirm")
        }
        uiState.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag(AuthTags.OTP_ERROR),
            )
        }
        Spacer(Modifier.height(12.dp))
        TextButton(
            onClick = viewModel::resend,
            enabled = uiState.secondsLeft == 0 && !uiState.loading,
            modifier = Modifier.testTag(AuthTags.OTP_RESEND),
        ) {
            Text(
                text =
                    if (uiState.secondsLeft > 0) {
                        "Resend code (in ${uiState.secondsLeft} s)"
                    } else {
                        "Resend code"
                    },
            )
        }
    }
}
