package com.enchanted.app.data.remote

import com.enchanted.app.data.SettingsDataStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

/**
 * OkHttp interceptor that adds an Authorization: Bearer <token>
 * header to every request, if a token is configured in settings.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking {
            settingsDataStore.getBearerToken()
        }

        if (token.isNullOrBlank()) {
            // Log a warning – missing token will cause 401 from NVIDIA NIM.
            Log.w("AuthInterceptor", "Bearer token is missing; proceeding without Authorization header. This will likely cause authentication errors.")
            return chain.proceed(chain.request())
        }

        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()

        return chain.proceed(request)
    }
}
