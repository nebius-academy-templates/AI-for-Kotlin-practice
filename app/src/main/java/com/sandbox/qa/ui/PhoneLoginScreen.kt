package com.sandbox.qa.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PhoneLoginScreen(
    onContinue: (String) -> Unit,
    viewModel: PhoneLoginViewModel = viewModel(factory = PhoneLoginViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.otpRequestedFor) {
        uiState.otpRequestedFor?.let { phone ->
            viewModel.onOtpRequestHandled()
            onContinue(phone)
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
            text = "Sign in with your phone number",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.testTag(AuthTags.PHONE_TITLE),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "We will send a code to confirm your number",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(AuthTags.PHONE_SUBTITLE),
        )
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                // Transient popup visibility, deliberately NOT ViewModel state
                // (same class as keyboard focus: it dies with the composition).
                var regionMenuOpen by remember { mutableStateOf(false) }
                Text(
                    text = "${uiState.region.flag} ${uiState.region.code}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier =
                        Modifier
                            .clickable { regionMenuOpen = true }
                            .testTag(AuthTags.PHONE_COUNTRY_CODE),
                )
                DropdownMenu(
                    expanded = regionMenuOpen,
                    onDismissRequest = { regionMenuOpen = false },
                    // The dropdown renders in its own popup window, which does
                    // NOT inherit testTagsAsResourceId from the activity root;
                    // without this flag the option tags never become
                    // resource-ids and locators silently stop matching.
                    modifier = Modifier.semantics { testTagsAsResourceId = true },
                ) {
                    PHONE_REGIONS.forEach { region ->
                        DropdownMenuItem(
                            text = { Text("${region.flag} ${region.code} ${region.name}") },
                            onClick = {
                                viewModel.onRegionSelected(region)
                                regionMenuOpen = false
                            },
                            modifier =
                                Modifier.testTag(
                                    AuthTags.PHONE_REGION_OPTION_PREFIX + region.code.removePrefix("+"),
                                ),
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            OutlinedTextField(
                value = uiState.phone,
                onValueChange = viewModel::onPhoneChange,
                label = { Text("Phone number") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(AuthTags.PHONE_INPUT),
            )
        }
        TextButton(
            onClick = viewModel::useTestNumber,
            modifier = Modifier.testTag(AuthTags.PHONE_TEST_NUMBER),
        ) {
            Text("Use test number $TEST_PHONE_NUMBER")
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = viewModel::submit,
            enabled = !uiState.loading,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(AuthTags.PHONE_CONTINUE),
        ) {
            Text("Next")
        }
        uiState.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag(AuthTags.PHONE_ERROR),
            )
        }
    }
}
