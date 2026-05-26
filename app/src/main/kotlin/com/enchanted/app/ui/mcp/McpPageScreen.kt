package com.enchanted.app.ui.mcp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * High‑level screen that could combine skill selection, execution and result
 * display. Currently it composes the dropdown screen and shows a placeholder
 * for the skill output.
 */
@Composable
fun McpPageScreen() {
    val result = remember { mutableStateOf("<no result>") }

    Column {
        McpDropdownScreen()
        Spacer(modifier = Modifier.height(24.dp))
        Text("Result:")
        Text(result.value)
        // In a real implementation the button in McpDropdownScreen would update this state.
        Button(onClick = { result.value = "Executed skill successfully (stub)" }) {
            Text("Show Stub Result")
        }
    }
}
