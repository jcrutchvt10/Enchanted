package com.enchanted.app.ui.chat.components.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enchanted.app.service.ClipboardService
import com.enchanted.app.ui.theme.CodeBackgroundDark
import com.enchanted.app.ui.theme.CodeBackgroundLight

@Composable
fun CodeBlock(
    code: String,
    modifier: Modifier = Modifier,
    clipboardService: ClipboardService? = null
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bgColor = if (isDark) CodeBackgroundDark else CodeBackgroundLight

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .background(bgColor)
    ) {
        // Code block header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isDark) Color(0xFF242428) else Color(0xFFE5E5EA)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Code",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = {
                    clipboardService?.copyToClipboard(code)
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy code to clipboard",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Code content with line numbers
        val codeLines = code.split("\n")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            // Line numbers column
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                codeLines.forEachIndexed { index, _ ->
                    Text(
                        text = "${index + 1}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 18.sp,
                        color = if (isDark) Color(0xFFE0E0E0).copy(alpha = 0.4f)
                                else Color(0xFF1C1C1E).copy(alpha = 0.4f)
                    )
                }
            }
            // Code text column
            Column {
                codeLines.forEach { line ->
                    Text(
                        text = line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = if (isDark) Color(0xFFE0E0E0) else Color(0xFF1C1C1E)
                    )
                }
            }
        }
    }
}

// Helper extension for luminance
private fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}
