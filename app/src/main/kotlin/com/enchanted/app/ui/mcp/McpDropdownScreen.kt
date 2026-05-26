package com.enchanted.app.ui.mcp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Demonstrates the skill selector dropdown together with an action button.
 * The actual MCP call is not performed – this screen is a placeholder that can be
 * wired to [McpClient] later.
 */
@Composable
fun McpDropdownScreen() {
    val selectedSkill = remember { mutableStateOf("") }

    Column {
        SkillSelectorScreen(selectedSkill = selectedSkill)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                // TODO: invoke McpClient.executeSkill with selectedSkill.value
            },
            enabled = selectedSkill.value.isNotBlank()
        ) {
            Text("Execute ${selectedSkill.value}")
        }
    }
}
