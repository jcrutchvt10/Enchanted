package com.enchanted.app.ui.studio

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

// Data class for saved artwork
data class Artwork(
    val id: String,
    val bitmap: Bitmap,
    val name: String,
    val timestamp: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    onNavigateBack: () -> Unit = {}
) {
    // Drawing state
    var paths by remember { mutableStateOf(listOf<Path>()) }
    var currentPath by remember { mutableStateOf(Path()) }

    // Tool selection
    var selectedTool by remember { mutableStateOf("draw") }

    // AI tools state
    var aiPrompt by remember { mutableStateOf("") }
    var generatedImage by remember { mutableStateOf<Bitmap?>(null) }

    // Asset management state
    var savedArtworks by remember { mutableStateOf(listOf<Artwork>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Studio") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTool == "draw",
                    onClick = { selectedTool = "draw" },
                    icon = { Icon(Icons.Default.Draw, "Draw") },
                    label = { Text("Draw") }
                )
                NavigationBarItem(
                    selected = selectedTool == "ai",
                    onClick = { selectedTool = "ai" },
                    icon = { Icon(Icons.Default.AutoAwesome, "AI") },
                    label = { Text("AI") }
                )
                NavigationBarItem(
                    selected = selectedTool == "assets",
                    onClick = { selectedTool = "assets" },
                    icon = { Icon(Icons.Default.Folder, "Assets") },
                    label = { Text("Assets") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTool) {
                "draw" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, _ ->
                                        val point = change.position
                                        currentPath.moveTo(point.x, point.y)
                                        currentPath.lineTo(point.x, point.y)
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentPath = Path().apply {
                                                moveTo(offset.x, offset.y)
                                            }
                                        },
                                        onDragEnd = {
                                            paths = paths + currentPath
                                            currentPath = Path()
                                        }
                                    ) { change, _ ->
                                        val point = change.position
                                        currentPath.lineTo(point.x, point.y)
                                    }
                                }
                        ) {
                            paths.forEach { path ->
                                drawPath(
                                    path = path,
                                    color = Color.Black,
                                    style = Stroke(width = 5f)
                                )
                            }
                            drawPath(
                                path = currentPath,
                                color = Color.Black,
                                style = Stroke(width = 5f)
                            )
                        }
                    }
                }

                "ai" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        OutlinedTextField(
                            value = aiPrompt,
                            onValueChange = { aiPrompt = it },
                            label = { Text("AI Prompt") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { generateArtwork(aiPrompt) { bitmap ->
                                generatedImage = bitmap
                            } },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Generate")
                        }
                        Text(
                            text = "Placeholder — AI generation not yet implemented",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        generatedImage?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Generated Art",
                                modifier = Modifier
                                    .padding(top = 16.dp)
                                    .size(200.dp)
                            )
                        }
                    }
                }

                "assets" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(savedArtworks) { artwork ->
                                ArtworkItem(
                                    artwork = artwork,
                                    onClick = { /* Load artwork into canvas */ }
                                )
                            }
                        }
                        Button(
                            onClick = { exportArtwork() },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Export")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtworkItem(
    artwork: Artwork,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(8.dp)
    ) {
        Image(
            bitmap = artwork.bitmap.asImageBitmap(),
            contentDescription = artwork.name,
            modifier = Modifier.size(150.dp)
        )
        Text(
            text = artwork.name,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// Placeholder functions for AI and asset management
private fun generateArtwork(prompt: String, onResult: (Bitmap) -> Unit) {
    // TODO: Integrate with AI API (e.g., Stable Diffusion, TensorFlow Lite)
    // For now, return a placeholder bitmap
    val placeholderBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
    onResult(placeholderBitmap)
}

private fun exportArtwork() {
    // TODO: Implement export functionality (PNG, JPEG, etc.)
}
