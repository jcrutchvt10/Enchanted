package com.enchanted.app

import android.app.Application
import com.google.crypto.tink.config.TinkConfig
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for the Enchanted app.
 *
 * The crash was caused by a missing initialization of the Tink library, which is used
 * indirectly by the Model Context Protocol (MCP) SDK. The MCP SDK relies on Tink for
 * cryptographic operations. When the app starts, the SDK attempts to instantiate
 * `KmsEnvelopeAeadKeyManager2`, which in turn expects Tink to be initialized. Without
 * calling `TinkConfig.register()` the static initialization of Tink fails, resulting in
 * a `NullPointerException` inside `XChaCha20Poly1305ParametersVariant.attachBaseContext`.
 *
 * By overriding `onCreate` and invoking `TinkConfig.register()` we ensure that the Tink
 * provider registry is set up before any cryptographic primitives are used.
 */
@HiltAndroidApp
class EnchantedApplication : Application() {
	override fun onCreate() {
		super.onCreate()
		// Initialise Tink early to avoid crashes in MCP SDK components.
		TinkConfig.register()
	}
}
