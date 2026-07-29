package com.sandbox.qa.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

/**
 * Wraps content shown in its OWN window (a Dialog or a dropdown popup) so
 * its testTags become UiAutomator resource-ids. Popup windows do not inherit
 * `testTagsAsResourceId` from the activity root, so without this the option
 * tags inside them are invisible to id-based locators - a real trap this
 * sandbox keeps on purpose (see the region picker). Use it once at the top
 * of any dialog/popup subtree.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TestTagWindow(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
    ) {
        content()
    }
}
