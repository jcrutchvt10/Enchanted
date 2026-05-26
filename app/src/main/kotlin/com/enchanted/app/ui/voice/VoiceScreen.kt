package com.enchanted.app.ui.voice

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.enchanted.app.ui.chat.components.ModelSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(
    onNavigateBack: () -> Unit,
    viewModel: VoiceViewModel = hiltViewModel()
) {
    val isListening by viewModel.isListening.collectAsState()
    val transcribedText by viewModel.transcribedText.collectAsState()
    val models by viewModel.models.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()

    val micColor by animateColorAsState(
        targetValue = if (isListening) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
        label = "micColor"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Input") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        val micScale by animateFloatAsState(
            targetValue = if (isListening) 1.08f else 1.0f,
            animationSpec = tween(durationMillis = 300),
            label = "micScale"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Mic button wrapped in a Card for elevation and rounded corners
            Card(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .graphicsLayer(scaleX = micScale, scaleY = micScale),
                colors = CardDefaults.cardColors(containerColor = micColor.copy(alpha = 0.2f)),
                shape = CircleShape,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                IconButton(
                    onClick = {
                        if (isListening) {
                            viewModel.stopListening()
                            transcribedText.let { text ->
                                if (text.isNotBlank()) {
                                    viewModel.sendVoiceMessage(text)
                                }
                            }
                        } else {
                            viewModel.startListening()
                        }
                    },
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = micColor)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = if (isListening) "Stop listening" else "Start listening",
                        modifier = Modifier.size(48.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isListening) "Listening..." else "Tap to speak",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Transcribed text area
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = transcribedText.ifEmpty { "Your speech will appear here..." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (transcribedText.isEmpty())
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp),
                    fontStyle = if (transcribedText.isEmpty()) FontStyle.Italic else FontStyle.Normal
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Model selector at bottom
            if (models.isNotEmpty()) {
                ModelSelector(
                    models = models,
                    selectedModel = selectedModel,
                    onSelectModel = { if (it != null) viewModel.selectModel(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
