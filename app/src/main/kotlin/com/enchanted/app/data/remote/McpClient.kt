package com.enchanted.app.data.remote

import android.util.Log
import com.enchanted.app.data.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A thin wrapper around the Model Context Protocol (MCP) Kotlin SDK.
 *
 * The actual SDK provides a client capable of authenticating via a bearer token and
 * invoking skills. For the purpose of this project we expose a simple suspend function
 * `executeSkill` that forwards the request to the SDK. The implementation is kept
 * deliberately minimal – it can be expanded later to handle pagination, error
 * handling, or custom headers.
 */
@Singleton
class McpClient @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val json: Json
) {
    // Placeholder for the real SDK client. The concrete type from the SDK is
    // `io.modelcontextprotocol.client.McpClient`. We keep the reference as `Any`
    // to avoid compile‑time dependency on the exact class name while still allowing
    // future replacement with the real type.
    private var sdkClient: Any? = null

    /**
     * Lazily creates the underlying SDK client using the stored endpoint URL.
     * The SDK typically expects a base URL and an optional bearer token.
     */
    private suspend fun ensureClient() {
        if (sdkClient == null) {
            // Re‑use the existing Ollama URI preference as the MCP endpoint for now.
            val endpoint = settingsDataStore.getOllamaUri()
            // The real SDK client would be instantiated like:
            // sdkClient = io.modelcontextprotocol.client.McpClient(endpoint)
            // Since we cannot reference the class directly without the exact import,
            // we store a placeholder object. Calls to `executeSkill` will currently
            // throw a NotImplementedError, signalling that integration is pending.
            sdkClient = endpoint // simple placeholder
        }
    }

    /**
     * Executes a skill identified by [skillId] with the provided JSON [input].
     * Returns the raw JSON response from the MCP service.
     *
     * NOTE: The current implementation is a stub – it throws a [NotImplementedError]
     * to indicate that the real SDK call needs to be wired up.
     */
    suspend fun executeSkill(skillId: String, input: JsonElement): JsonElement = withContext(Dispatchers.IO) {
        ensureClient()
        // TODO: Replace the following stub with:
        // (sdkClient as io.modelcontextprotocol.client.McpClient).executeSkill(skillId, input)
        Log.w("McpClient", "executeSkill called for skillId=$skillId – stub implementation")
        throw NotImplementedError("MCP SDK integration not yet implemented")
    }
}
