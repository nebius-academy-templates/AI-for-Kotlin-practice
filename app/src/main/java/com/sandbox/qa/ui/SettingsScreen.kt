package com.sandbox.qa.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.sandbox.qa.condition.ConditionConfig

/**
 * Sandbox state switchboard. Architecture exception, on purpose: this
 * screen reads and writes [ConditionConfig] snapshot state directly instead of
 * going through a ViewModel. ConditionConfig is a testability seam shared with
 * the adb [com.sandbox.qa.condition.ConditionReceiver]; a mid-session broadcast must
 * flip these switches instantly, and a StateFlow bridge could lose that.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sandbox states", modifier = Modifier.testTag("settings_title")) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
        ) {
            ConditionConfig.ALL.forEach { condition ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("condition_row_$condition"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = condition,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = ConditionConfig.isEnabled(condition),
                        onCheckedChange = { ConditionConfig.set(condition, it) },
                        modifier = Modifier.testTag("condition_switch_$condition"),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text =
                    "States can also be toggled from adb:\n" +
                        "adb shell am broadcast -n com.sandbox.qa/.condition.ConditionReceiver " +
                        "--es condition slow_backend_response --ez enabled true",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("settings_adb_hint"),
            )
        }
    }
}
