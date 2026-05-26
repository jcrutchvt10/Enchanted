package com.enchanted.app.ui.mcp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview

/**
 * Simple reusable dropdown for selecting a skill identifier.
 * The list of skills is currently hard‑coded for demonstration purposes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillSelectorScreen(
    selectedSkill: MutableState<String>,
    modifier: Modifier = Modifier
) {
    // In a real app this would be loaded from a repository or remote source.
    val skillOptions = listOf(
        "skill.chat",
        "skill.translate",
        "skill.imageGenerate"
    )

    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            value = selectedSkill.value,
            onValueChange = { /* no‑op – selection handled via menu */ },
            readOnly = true,
            label = { Text("Select Skill") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            skillOptions.forEach { skill ->
                DropdownMenuItem(
                    text = { Text(skill) },
                    onClick = {
                        selectedSkill.value = skill
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SkillSelectorPreview() {
    val selected = remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxWidth()) {
        SkillSelectorScreen(selectedSkill = selected)
    }
}
