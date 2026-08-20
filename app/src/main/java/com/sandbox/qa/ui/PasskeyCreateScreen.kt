package com.sandbox.qa.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * The fake local passkey creation the promo's "Create a passkey" leads to:
 * a spinner beat, a success beat, then navigation to location onboarding.
 * Tags: passkey_creating (spinner), passkey_created (success text).
 */
@Composable
fun PasskeyCreateScreen(
    onDone: () -> Unit,
    viewModel: PasskeyCreateViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.start() }
    LaunchedEffect(uiState.done) {
        if (uiState.done) onDone()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!uiState.created) {
            CircularProgressIndicator(modifier = Modifier.testTag("passkey_creating"))
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Creating your passkey...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = "✓",
                color = Lime,
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Passkey created",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.testTag("passkey_created"),
            )
        }
    }
}
