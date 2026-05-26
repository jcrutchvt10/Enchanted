package com.enchanted.app.ui.sync

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SyncQueueScreen(
    viewModel: SyncViewModel = viewModel()
) {
    val pendingList by viewModel.pendingList.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Simple UI to trigger a dummy enqueue for demonstration.
        Button(onClick = { viewModel.enqueueMaterialization("exampleDir", "session123") }) {
            Text("Enqueue Dummy Materialisation")
        }
        LazyColumn {
            items(pendingList) { item ->
                Text(text = "${item.directory} – ${item.sessionId}")
            }
        }
    }
}
