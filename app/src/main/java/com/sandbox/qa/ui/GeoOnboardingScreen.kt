package com.sandbox.qa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun GeoOnboardingScreen(
    onDone: () -> Unit,
    viewModel: GeoOnboardingViewModel = viewModel(factory = GeoOnboardingViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.granted) {
        if (uiState.granted) onDone()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(180.dp)
                        .align(Alignment.CenterHorizontally)
                        .background(Lime, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "📍", style = MaterialTheme.typography.displayLarge)
            }
            Spacer(Modifier.height(32.dp))
            Text(
                text = "Turn on location",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.testTag("geo_title"),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text =
                    "We use your location to find drivers nearby, " +
                        "detect pickup and drop-off addresses, estimate trip " +
                        "duration, and keep you safe",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("geo_description"),
            )
            uiState.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("geo_error"),
                )
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = viewModel::requestGeolocation,
                enabled = !uiState.processing,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("geo_enable_button"),
            ) {
                Text("Enable location")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = viewModel::requestGeolocation,
                enabled = !uiState.processing,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("geo_manual_button"),
            ) {
                Text("Choose location manually")
            }
        }

        if (uiState.processing) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .testTag("geo_processing_overlay"),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Processing request…",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.testTag("geo_processing_text"),
                    )
                }
            }
        }
    }
}
