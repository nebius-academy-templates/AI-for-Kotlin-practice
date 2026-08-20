package com.sandbox.qa.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Pure navigation screen: static promo content, two exits. "Create a
 * passkey" leads into the fake creation flow ([PasskeyCreateScreen]), Skip
 * goes straight to location onboarding. It carries no business state, so it
 * deliberately stays a plain composable with no ViewModel.
 */
@Composable
fun PasskeyPromoScreen(
    onCreate: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Fast and secure sign-in with a passkey",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.testTag("passkey_title"),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "No more waiting for a code",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("passkey_subtitle"),
        )
        Spacer(Modifier.height(24.dp))
        PasskeyBullet(
            emoji = "👆",
            text =
                "Sign in with your fingerprint, face, or password. " +
                    "We do not collect this information",
            tag = "passkey_bullet_biometrics",
        )
        Spacer(Modifier.height(16.dp))
        PasskeyBullet(
            emoji = "🛡️",
            text = "A passkey is safer because it is stored only in your device account",
            tag = "passkey_bullet_security",
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onCreate,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag("passkey_create_button"),
        ) {
            Text("Create a passkey")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onSkip,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag("passkey_skip_button"),
        ) {
            Text("Skip")
        }
    }
}

@Composable
private fun PasskeyBullet(
    emoji: String,
    text: String,
    tag: String,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = emoji, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag(tag),
        )
    }
}
