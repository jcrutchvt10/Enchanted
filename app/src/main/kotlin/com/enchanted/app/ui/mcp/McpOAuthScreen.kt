package com.enchanted.app.ui.mcp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Placeholder UI for an OAuth authentication flow required by some MCP services.
 * The implementation would typically launch a WebView or Custom Tab and handle
 * the redirect URI. For now we simply display a message and a button to simulate
 * completion.
 */
@Composable
fun McpOAuthScreen(onAuthComplete: () -> Unit = {}) {
    Column {
        Text("OAuth authentication is required to access MCP services.")
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { onAuthComplete() }) {
            Text("Simulate Auth Completion")
        }
    }
}
