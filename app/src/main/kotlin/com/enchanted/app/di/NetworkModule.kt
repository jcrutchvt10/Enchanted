package com.enchanted.app.di

import com.enchanted.app.data.SettingsDataStore
import com.enchanted.app.data.remote.AuthInterceptor
import com.enchanted.app.data.remote.DynamicBaseUrlInterceptor
import com.enchanted.app.data.remote.OllamaApi
import com.enchanted.app.data.remote.NimClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.BufferedSink
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        dynamicBaseUrlInterceptor: DynamicBaseUrlInterceptor,
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(dynamicBaseUrlInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor { chain ->
                val request = chain.request()
                val body = request.body
                if (body != null) {
                    val contentType = body.contentType()
                    if (contentType != null && contentType.subtype == "json") {
                        // Force Content-Type to exactly "application/json" without charset
                        val newBody = object : RequestBody() {
                            override fun contentType(): MediaType = "application/json".toMediaType()
                            override fun contentLength(): Long = body.contentLength()
                            override fun writeTo(sink: BufferedSink) = body.writeTo(sink)
                        }
                        return@addInterceptor chain.proceed(
                            request.newBuilder()
                                .method(request.method, newBody)
                                .header("Content-Type", "application/json")
                                .build()
                        )
                    }
                }
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideOllamaApi(okHttpClient: OkHttpClient, json: Json): OllamaApi {
        val placeholderUrl = "http://placeholder.local/"

        return Retrofit.Builder()
            .baseUrl(placeholderUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OllamaApi::class.java)
    }

    @Provides
    @Singleton
    fun provideNimClient(ollamaApi: OllamaApi, settingsDataStore: SettingsDataStore): NimClient {
        return NimClient(ollamaApi, settingsDataStore)
    }
}
