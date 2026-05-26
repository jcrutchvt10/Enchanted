package com.enchanted.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val ollamaUri by viewModel.ollamaUri.collectAsState()
    val bearerToken by viewModel.bearerToken.collectAsState()
    val systemPrompt by viewModel.systemPrompt.collectAsState()
    val userInitials by viewModel.userInitials.collectAsState()
    val colorScheme by viewModel.colorScheme.collectAsState()
    val defaultModel by viewModel.defaultModel.collectAsState()
    val pingInterval by viewModel.pingInterval.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    // Local editing state (saved on "Save")
    var localUri by remember(ollamaUri) { mutableStateOf(ollamaUri) }
    var localToken by remember(bearerToken) { mutableStateOf(bearerToken ?: "") }
    var localSystemPrompt by remember(systemPrompt) { mutableStateOf(systemPrompt) }
    var localInitials by remember(userInitials) { mutableStateOf(userInitials) }
    var localPing by remember(pingInterval) { mutableStateOf(pingInterval.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            // Save: strip trailing slash + persist + test + reload models
                            val cleanUri = localUri.trimEnd('/')
                            viewModel.setOllamaUri(cleanUri)
                            viewModel.setBearerToken(localToken.ifBlank { null })
                            viewModel.setSystemPrompt(localSystemPrompt)
                            viewModel.setUserInitials(localInitials)
                            viewModel.setPingInterval(localPing.toFloatOrNull() ?: 5f)
                            viewModel.saveAndReload()
                        },
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── Connection status banner ──
            AnimatedVisibility(
                visible = connectionStatus !is ConnectionStatus.Unknown,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val (icon, color, message) = when (val s = connectionStatus) {
                    is ConnectionStatus.Testing -> Triple(
                        Icons.Default.Refresh,
                        MaterialTheme.colorScheme.primary,
                        "Testing connection..."
                    )
                    is ConnectionStatus.Connected -> Triple(
                        Icons.Default.CheckCircle,
                        Color(0xFF34C759),
                        "Connected successfully"
                    )
                    is ConnectionStatus.Error -> Triple(
                        Icons.Default.Error,
                        MaterialTheme.colorScheme.error,
                        s.message
                    )
                    else -> Triple(null, Color.Transparent, "")
                }

                if (icon != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = color
                        )
                    }
                }
            }

            // ════════════════════════════════════════
            //  SECTION: SERVER
            // ════════════════════════════════════════
            SectionHeader("Server")

            OutlinedTextField(
                value = localUri,
                onValueChange = { 
                    localUri = it
                    viewModel.setOllamaUri(it.trimEnd('/'))
                },
                label = { Text("Server URL") },
                placeholder = { Text("https://integrate.api.nvidia.com/v1") },
                supportingText = {
                    Text("Your Ollama server or any OpenAI-compatible API endpoint")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = localToken,
                onValueChange = { 
                    localToken = it
                    viewModel.setBearerToken(it.ifBlank { null })
                },
                label = { Text("Bearer Token (optional)") },
                placeholder = { Text("sk-...") },
                supportingText = {
                    Text("API key for authenticated endpoints (NVIDIA NIM, OpenAI, etc.)")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Test connection button
            OutlinedButton(
                onClick = { viewModel.testConnection() },
                enabled = connectionStatus !is ConnectionStatus.Testing,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (connectionStatus is ConnectionStatus.Testing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Test Connection")
            }

            // Fetch models button (visible after successful connection)
            AnimatedVisibility(visible = connectionStatus is ConnectionStatus.Connected) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = { viewModel.testConnection() }, // testConnection already refreshes models
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh models")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fetch Models")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // ════════════════════════════════════════
            //  SECTION: CHAT DEFAULTS
            // ════════════════════════════════════════
            SectionHeader("Chat Defaults")

            OutlinedTextField(
                value = localSystemPrompt,
                onValueChange = { localSystemPrompt = it },
                label = { Text("System Prompt") },
                placeholder = { Text("You are a helpful assistant.") },
                supportingText = {
                    Text("Prepended to every conversation")
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = localPing,
                onValueChange = { localPing = it },
                label = { Text("Ping Interval (seconds)") },
                supportingText = {
                    Text("How often to check server reachability. 0 = disable.")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // ════════════════════════════════════════
            //  SECTION: APPEARANCE & PROFILE
            // ════════════════════════════════════════
            SectionHeader("Appearance & Profile")

            OutlinedTextField(
                value = localInitials,
                onValueChange = { localInitials = it },
                label = { Text("Your Initials") },
                placeholder = { Text("AM") },
                supportingText = {
                    Text("Shown on your chat messages")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // ════════════════════════════════════════
            //  SECTION: DANGER ZONE
            // ════════════════════════════════════════
            SectionHeader("Data", color = MaterialTheme.colorScheme.error)

            Button(
                onClick = { viewModel.clearAllData() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear All Data")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App info
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Enchanted for Android v1.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Ollama-compatible LLM chat client",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Port of Enchanted (iOS) by Augustinas Malinauskas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = color,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}
