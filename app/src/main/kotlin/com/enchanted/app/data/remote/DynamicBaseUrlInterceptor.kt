package com.enchanted.app.data.remote

import com.enchanted.app.data.SettingsDataStore
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

/**
 * OkHttp interceptor that rewrites each request's URL to use the
 * Ollama server URL configured in settings (instead of the placeholder
 * used in the Retrofit @BaseUrl annotation).
 */
@Singleton
class DynamicBaseUrlInterceptor @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Read the configured URL from settings (blocking read on OkHttp's thread)
        val configuredUrl = runBlocking {
            settingsDataStore.getOllamaUri()
        }

        if (configuredUrl.isBlank()) return chain.proceed(originalRequest)

        var baseUrl = configuredUrl.trimEnd('/')
        if (!baseUrl.startsWith("http")) {
            baseUrl = "http://$baseUrl"
        }

        // Parse the base URL; if malformed, log a warning and skip rewriting.
        val baseHttpUrl = baseUrl.toHttpUrlOrNull()
        if (baseHttpUrl == null) {
            Log.w("DynamicUrl", "Configured base URL is malformed: $baseUrl. Skipping URL rewrite.")
            return chain.proceed(originalRequest)
        }

        // Build the final URL by appending the request's path to the base URL.
        // Using `resolve` with a leading '/' would replace the base path (e.g. drop "/v1").
        // Instead we construct a new URL that preserves the base path and adds the request path.
        val pathToAppend = originalRequest.url.encodedPath.removePrefix("/")
        val urlBuilder = baseHttpUrl.newBuilder()
        if (pathToAppend.isNotEmpty()) {
            urlBuilder.addEncodedPathSegments(pathToAppend)
        }
        // Preserve query parameters if present.
        if (originalRequest.url.encodedQuery != null) {
            urlBuilder.encodedQuery(originalRequest.url.encodedQuery)
        }
        val finalUrl = urlBuilder.build()

        val newRequest = originalRequest.newBuilder()
            .url(finalUrl)
            .build()

        // Log the URL rewrite for debugging purposes.
        Log.d("DynamicUrl", "Rewriting ${originalRequest.url} -> $finalUrl")

        return chain.proceed(newRequest)
    }
}
