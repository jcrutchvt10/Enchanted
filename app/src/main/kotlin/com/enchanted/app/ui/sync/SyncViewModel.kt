package com.enchanted.app.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * ViewModel that mirrors the sync‑queue logic from OpenChamber's `sync-context.tsx`.
 * It tracks pending materialisations per (directory, sessionId) pair and enforces a cooldown
 * of {@code SESSION_MATERIALIZATION_COOLDOWN_MS} before allowing another enqueue for the same key.
 */
class SyncViewModel : ViewModel() {
    companion object {
        private const val SESSION_MATERIALIZATION_COOLDOWN_MS = 5_000L
    }

    data class PendingMaterialization(
        val directory: String,
        val sessionId: String,
        val enqueuedAt: Long = System.currentTimeMillis()
    )

    // Internal map keyed by "directory:sessionId"
    private val pendingMap = ConcurrentHashMap<String, PendingMaterialization>()

    // Exposed immutable list of pending materialisations for UI consumption.
    private val _pendingList = MutableStateFlow<List<PendingMaterialization>>(emptyList())
    val pendingList: StateFlow<List<PendingMaterialization>> = _pendingList

    /**
     * Enqueue a materialisation request. If a request for the same key was enqueued within the
     * cooldown period, the call is ignored (mirroring the behaviour of the original JS code).
     */
    fun enqueueMaterialization(directory: String, sessionId: String) {
        if (directory.isBlank() || directory == "global" || sessionId.isBlank()) return
        val key = "${directory}:${sessionId}"
        val existing = pendingMap[key]
        val now = System.currentTimeMillis()
        if (existing != null && now - existing.enqueuedAt < SESSION_MATERIALIZATION_COOLDOWN_MS) {
            // Cool‑down active – ignore duplicate enqueue.
            return
        }
        val pending = PendingMaterialization(directory, sessionId, now)
        pendingMap[key] = pending
        // Update the exposed list on the main thread.
        _pendingList.value = pendingMap.values.toList()

        // Simulate async materialisation work on a micro‑task (similar to Promise.resolve().then(...)).
        viewModelScope.launch {
            // In a real implementation we would call the server API here.
            // For now we just wait a short moment and then remove the entry.
            delay(100) // tiny delay to mimic async work
            // After processing, remove from pending.
            pendingMap.remove(key)
            _pendingList.value = pendingMap.values.toList()
        }
    }
}
