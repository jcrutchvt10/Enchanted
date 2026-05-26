package com.enchanted.app.ui.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.enchanted.app.domain.model.LanguageModel

@Composable
fun Header(
    onMenuTap: () -> Unit,
    onNewConversationTap: () -> Unit,
    models: List<LanguageModel>,
    selectedModel: LanguageModel?,
    onSelectModel: (LanguageModel?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenuTap) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        if (models.isNotEmpty()) {
            ModelSelector(
                models = models,
                selectedModel = selectedModel,
                onSelectModel = onSelectModel,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        IconButton(onClick = onNewConversationTap) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "New conversation",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
