package com.enchanted.app.ui.chat.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.enchanted.app.domain.model.ConversationState

@Composable
fun ConversationStatusView(
    state: ConversationState,
    modifier: Modifier = Modifier
) {
    if (state is ConversationState.Loading || state is ConversationState.Error) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (state) {
                is ConversationState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Generating",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontStyle = FontStyle.Italic
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    BouncingDots(
                        dotSize = 4.dp,
                        dotColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is ConversationState.Error -> {
                    val displayText = when {
                        // DNS / host resolution errors → show a concise message
                        state.message.contains("Cannot reach", ignoreCase = true) ||
                        state.message.contains("resolve host", ignoreCase = true) ||
                        state.message.contains("Unable to resolve", ignoreCase = true) ||
                        state.message.contains("Network is unreachable", ignoreCase = true) ->
                            "Network error: cannot reach the API server.\nCheck your connection or the API URL in Settings."

                        // Everything else shows the raw message (capped)
                        else -> "Error: ${state.message}"
                    }
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                else -> {}
            }
        }
    }
}
