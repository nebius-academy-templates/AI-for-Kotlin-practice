package com.sandbox.qa.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Create/Edit profile screen, opened from the drawer's profile header. When
 * the profile is still anonymous the title reads "Create your profile" and
 * the fields are empty; once a name exists it reads "Edit profile" and the
 * fields are prefilled. First/last name and email are free text; the city
 * opens a searchable picker; the phone is masked and read-only. Save writes
 * to the persistent ProfileStore, so the drawer header shows the new name
 * and it survives a restart; the reset broadcast returns it to anonymous.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    onBack: () -> Unit,
    viewModel: ProfileEditViewModel = viewModel(factory = ProfileEditViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsState()
    var cityPickerOpen by remember { mutableStateOf(false) }

    val creating = uiState.firstName.isBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (creating) "Create your profile" else "Edit profile",
                        modifier = Modifier.testTag("profile_title"),
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("profile_back_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Decorative avatar placeholder: photo upload needs storage
            // permissions and would break the no-dialogs test setup.
            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(96.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .testTag("profile_avatar"),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "🙂", style = MaterialTheme.typography.headlineLarge)
            }

            OutlinedTextField(
                value = uiState.firstName,
                onValueChange = viewModel::onFirstNameChange,
                label = { Text("First name") },
                singleLine = true,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("profile_first_name_input"),
            )
            OutlinedTextField(
                value = uiState.lastName,
                onValueChange = viewModel::onLastNameChange,
                label = { Text("Last name") },
                singleLine = true,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("profile_last_name_input"),
            )
            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("profile_email_input"),
            )

            // City is a read-only field styled like the others; tapping it
            // opens the search dialog. readOnly + a click overlay keeps the
            // OutlinedTextField look without a text keyboard.
            Box {
                OutlinedTextField(
                    value = uiState.city,
                    onValueChange = {},
                    label = { Text("City") },
                    readOnly = true,
                    singleLine = true,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("profile_city_field"),
                )
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .clickable { cityPickerOpen = true }
                            .testTag("profile_city_click"),
                )
            }

            OutlinedTextField(
                value = uiState.phoneMasked,
                onValueChange = {},
                label = { Text("Phone number") },
                readOnly = true,
                enabled = false,
                singleLine = true,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("profile_phone_field"),
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = viewModel::save,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("profile_save_button"),
            ) {
                Text("Save")
            }
            if (uiState.saved) {
                Text(
                    text = "Profile saved",
                    color = Lime,
                    modifier =
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .testTag("profile_saved_label"),
                )
            }
        }
    }

    if (cityPickerOpen) {
        CityPickerDialog(
            onPick = {
                viewModel.onCitySelected(it)
                cityPickerOpen = false
            },
            onDismiss = { cityPickerOpen = false },
            matching = viewModel::citiesMatching,
        )
    }
}

/**
 * Full-screen city search: a query field over a filtered list, with an
 * explicit empty state. NB the dialog renders in its own window, so it must
 * set testTagsAsResourceId itself (same popup caveat as the region and city
 * dropdowns) - done via the shared TestTagWindow wrapper.
 */
@Composable
private fun CityPickerDialog(
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
    matching: (String) -> List<ProfileCity>,
) {
    var query by remember { mutableStateOf("") }
    val results = matching(query)

    Dialog(onDismissRequest = onDismiss) {
        TestTagWindow {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "City",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("city_picker_close"),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("City") },
                    singleLine = true,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("city_picker_search"),
                )
                Spacer(Modifier.height(12.dp))
                if (results.isEmpty()) {
                    Text(
                        text = "City not found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp)
                                .testTag("city_picker_empty"),
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(results) { city ->
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { onPick(city.name) }
                                        .padding(vertical = 12.dp)
                                        .testTag("city_option_${city.name}"),
                            ) {
                                Text(text = city.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = city.region,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
