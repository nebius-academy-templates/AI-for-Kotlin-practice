package com.sandbox.qa.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

private data class SupportFaq(
    val question: String,
    val answer: String,
)

/** A compact FAQ: each row expands locally, so every item provides an actual answer. */
private val FAQ_ITEMS =
    listOf(
        SupportFaq(
            question = "How do I request a ride?",
            answer = "Enter a destination, choose a tariff, then tap Order. Pull down to refresh the tariff list.",
        ),
        SupportFaq(
            question = "How is the ride price calculated?",
            answer = "The sandbox shows a fixed estimate for every tariff before you order.",
        ),
        SupportFaq(
            question = "I left something in the car",
            answer = "Contact support and include the route and approximate ride time.",
        ),
        SupportFaq(
            question = "Where is the service available?",
            answer = "The current demo region is supported unless region_unavailable is enabled.",
        ),
    )

/**
 * Compact interactive support: FAQ rows expand to useful local answers and
 * one contact entry opens deterministic chat/email actions. The interaction
 * state is transient UI state (like a popup or keyboard focus), so this screen
 * deliberately needs no ViewModel and no external support backend.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(onBack: () -> Unit) {
    var expandedFaqIndex by remember { mutableStateOf<Int?>(null) }
    var contactDialogOpen by remember { mutableStateOf(false) }
    var contactStatus by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Support", modifier = Modifier.testTag("support_title")) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("support_back_button"),
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
                    .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "FAQ",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .padding(vertical = 12.dp)
                        .testTag("support_faq_header"),
            )
            FAQ_ITEMS.forEachIndexed { index, item ->
                val expanded = expandedFaqIndex == index
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedFaqIndex = if (expanded) null else index
                            }.padding(vertical = 14.dp)
                            .testTag("support_faq_item_${index + 1}"),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.question,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Text(if (expanded) "−" else "+")
                    }
                    if (expanded) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = item.answer,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("support_faq_answer_${index + 1}"),
                        )
                    }
                }
                HorizontalDivider()
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Contact",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .padding(vertical = 12.dp)
                        .testTag("support_contact_header"),
            )
            Text(
                text = "💬 Contact support",
                style = MaterialTheme.typography.bodyLarge,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            contactStatus = null
                            contactDialogOpen = true
                        }.padding(vertical = 14.dp)
                        .testTag("support_contact_item"),
            )
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))
        }
    }

    if (contactDialogOpen) {
        Dialog(onDismissRequest = { contactDialogOpen = false }) {
            TestTagWindow {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 6.dp,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("support_contact_dialog"),
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "Contact support",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Choose how you want to continue in this sandbox.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = {
                                contactStatus =
                                    "Sandbox chat started. A support specialist will reply here."
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .testTag("support_contact_chat"),
                        ) {
                            Text("Start sandbox chat")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                contactStatus = "Email draft prepared for support@sandbox.qa."
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .testTag("support_contact_email"),
                        ) {
                            Text("Email support")
                        }
                        contactStatus?.let { status ->
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = status,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("support_contact_status"),
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        TextButton(
                            onClick = { contactDialogOpen = false },
                            modifier =
                                Modifier
                                    .align(Alignment.End)
                                    .testTag("support_contact_close"),
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}
