package com.enchanted.app.ui.chat.components.markdown

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    content: String,
    modifier: Modifier = Modifier
) {
    // Simple markdown rendering - handles basic formatting
    // For production, consider using a markdown library like commonmark or compose-richtext
    val lines = content.split("\n")
    val codeBlocks = mutableListOf<String>()
    var inCodeBlock = false
    var codeBlockContent = StringBuilder()

    Column(modifier = modifier.padding(vertical = 4.dp, horizontal = 4.dp)) {
        var i = 0
        while (i < lines.size) {
            val line = lines[i]

            when {
                line.startsWith("```") && !inCodeBlock -> {
                    inCodeBlock = true
                    codeBlockContent = StringBuilder()
                }
                line.startsWith("```") && inCodeBlock -> {
                    inCodeBlock = false
                    CodeBlock(
                        code = codeBlockContent.toString().trimEnd(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
                inCodeBlock -> {
                    codeBlockContent.appendLine(line)
                }
                line.startsWith("### ") -> {
                    Text(
                        text = line.removePrefix("### "),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## "),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                line.startsWith("# ") -> {
                    Text(
                        text = line.removePrefix("# "),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    Text(
                        text = "  \u2022  ${line.removePrefix("- ").removePrefix("* ")}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
                line.matches(Regex("^\\d+\\.\\s.*")) -> {
                    val num = line.substringBefore(".")
                    val text = line.substringAfter(". ")
                    Text(
                        text = "  $num.  $text",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
                line.startsWith("> ") -> {
                    Text(
                        text = line.removePrefix("> "),
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                    )
                }
                line.isBlank() -> {
                    Spacer(modifier = Modifier.size(8.dp))
                }
                else -> {
                    // Inline formatting
                    val annotatedString = buildAnnotatedString {
                        var remaining = line
                        while (remaining.isNotEmpty()) {
                            when {
                                remaining.startsWith("**") -> {
                                    val end = remaining.indexOf("**", 2)
                                    if (end > 0) {
                                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append(remaining.substring(2, end))
                                        }
                                        remaining = remaining.substring(end + 2)
                                    } else {
                                        append(remaining)
                                        remaining = ""
                                    }
                                }
                                remaining.startsWith("*") && !remaining.startsWith("**") -> {
                                    val end = remaining.indexOf("*", 1)
                                    if (end > 0) {
                                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                            append(remaining.substring(1, end))
                                        }
                                        remaining = remaining.substring(end + 1)
                                    } else {
                                        append(remaining)
                                        remaining = ""
                                    }
                                }
                                remaining.startsWith("`") -> {
                                    val end = remaining.indexOf("`", 1)
                                    if (end > 0) {
                                        withStyle(SpanStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp
                                        )) {
                                            append(remaining.substring(1, end))
                                        }
                                        remaining = remaining.substring(end + 1)
                                    } else {
                                        append(remaining)
                                        remaining = ""
                                    }
                                }
                                else -> {
                                    val nextSpecial = listOf("**", "*", "`").mapNotNull {
                                        val idx = remaining.indexOf(it)
                                        if (idx >= 0) idx else null
                                    }.minOrNull()

                                    if (nextSpecial != null && nextSpecial > 0) {
                                        append(remaining.substring(0, nextSpecial))
                                        remaining = remaining.substring(nextSpecial)
                                    } else {
                                        append(remaining)
                                        remaining = ""
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
            i++
        }

        if (inCodeBlock) {
            CodeBlock(
                code = codeBlockContent.toString().trimEnd(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }
    }
}
