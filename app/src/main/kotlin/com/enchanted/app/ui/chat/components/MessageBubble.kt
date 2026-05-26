package com.enchanted.app.ui.chat.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.enchanted.app.domain.model.Message
import com.enchanted.app.ui.chat.components.markdown.MarkdownText
import com.enchanted.app.ui.theme.MessageBubbleAssistantDark
import com.enchanted.app.ui.theme.MessageBubbleAssistantLight
import com.enchanted.app.ui.theme.MessageBubbleUserDark
import com.enchanted.app.ui.theme.MessageBubbleUserLight

@Composable
fun MessageBubble(
    message: Message,
    userInitials: String,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"
    val bubbleColor = if (isUser) {
        if (MaterialTheme.colorScheme.background.luminance() < 0.5f)
            MessageBubbleUserDark else MessageBubbleUserLight
    } else {
        if (MaterialTheme.colorScheme.background.luminance() < 0.5f)
            MessageBubbleAssistantDark else MessageBubbleAssistantLight
    }
    val textColor = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // Assistant avatar
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AI",
                    color = Color.White,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            // Attached image
            if (message.imageData != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = message.imageData),
                    contentDescription = "Attached image",
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .size(width = 200.dp, height = 150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
            }

            // Think block
            if (message.hasThink) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (MaterialTheme.colorScheme.background.luminance() < 0.5f)
                                Color(0xFF1A1A2E) else Color(0xFFF0F0FF)
                        )
                        .padding(8.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Thinking",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (!message.thinkComplete) {
                                BouncingDots(
                                    dotSize = 4.dp,
                                    dotColor = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        message.think?.let { thinkText ->
                            if (thinkText.isNotEmpty()) {
                                Text(
                                    text = thinkText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Message content
            val showContent = isUser || message.realContent.isNotBlank() || (!message.hasThink && !message.done)
            
            if (showContent) {
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 16.dp
                            )
                        )
                        .background(bubbleColor)
                        .padding(12.dp)
                ) {
                    if (isUser) {
                        Text(
                            text = message.content,
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else if (message.realContent.isBlank() && !message.done) {
                        BouncingDots(
                            dotSize = 8.dp,
                            dotColor = textColor.copy(alpha = 0.5f)
                        )
                    } else {
                        MarkdownText(
                            content = message.realContent,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Error indicator
            if (message.error) {
                Text(
                    text = "⚠ Error generating response",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        if (isUser) {
            // User avatar
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userInitials.ifEmpty { "U" },
                    color = Color.White,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}
