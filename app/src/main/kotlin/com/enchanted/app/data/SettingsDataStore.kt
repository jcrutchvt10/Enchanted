package com.enchanted.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.enchanted.app.domain.model.AppColorScheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val OLLAMA_URI = stringPreferencesKey("ollamaUri")
        val OLLAMA_BEARER_TOKEN = stringPreferencesKey("ollamaBearerToken")
        val SYSTEM_PROMPT = stringPreferencesKey("systemPrompt")
        val USER_INITIALS = stringPreferencesKey("appUserInitials")
        val DEFAULT_OLLAMA_MODEL = stringPreferencesKey("defaultOllamaModel")
        val COLOR_SCHEME = stringPreferencesKey("colorScheme")
        val PING_INTERVAL = stringPreferencesKey("pingInterval")
    }

    val ollamaUri: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[OLLAMA_URI] ?: "https://integrate.api.nvidia.com/v1"
    }

    val bearerToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[OLLAMA_BEARER_TOKEN]
    }

    val systemPrompt: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SYSTEM_PROMPT] ?: ""
    }

    val userInitials: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[USER_INITIALS] ?: ""
    }

    val defaultModel: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[DEFAULT_OLLAMA_MODEL] ?: ""
    }

    val colorScheme: Flow<AppColorScheme> = context.dataStore.data.map { prefs ->
        try {
            AppColorScheme.valueOf(prefs[COLOR_SCHEME] ?: "SYSTEM")
        } catch (_: Exception) {
            AppColorScheme.SYSTEM
        }
    }

    val pingInterval: Flow<Float> = context.dataStore.data.map { prefs ->
        val value = prefs[PING_INTERVAL]
        if (value != null) value.toFloatOrNull() ?: 5f else 5f
    }

    suspend fun setOllamaUri(uri: String) {
        context.dataStore.edit { it[OLLAMA_URI] = uri }
    }

    suspend fun setBearerToken(token: String?) {
        context.dataStore.edit { if (token != null) it[OLLAMA_BEARER_TOKEN] = token else it.remove(OLLAMA_BEARER_TOKEN) }
    }

    suspend fun setSystemPrompt(prompt: String) {
        context.dataStore.edit { it[SYSTEM_PROMPT] = prompt }
    }

    suspend fun setUserInitials(initials: String) {
        context.dataStore.edit { it[USER_INITIALS] = initials }
    }

    suspend fun setDefaultModel(model: String) {
        context.dataStore.edit { it[DEFAULT_OLLAMA_MODEL] = model }
    }

    suspend fun setColorScheme(scheme: AppColorScheme) {
        context.dataStore.edit { it[COLOR_SCHEME] = scheme.name }
    }

    suspend fun setPingInterval(interval: Float) {
        context.dataStore.edit { it[PING_INTERVAL] = interval.toString() }
    }

    /** One-shot read for OkHttp interceptors (blocks on IO). */
    suspend fun getOllamaUri(): String {
        return context.dataStore.data.map { prefs ->
            prefs[OLLAMA_URI] ?: "https://integrate.api.nvidia.com/v1"
        }.first()
    }

    /** One-shot read for OkHttp interceptors (blocks on IO). */
    suspend fun getBearerToken(): String? {
        return context.dataStore.data.map { prefs ->
            prefs[OLLAMA_BEARER_TOKEN]
        }.first()
    }
}
