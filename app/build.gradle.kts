plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.enchanted.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.enchanted.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Enable multidex for large apps (19 DEX files indicates this is needed)
        multiDexEnabled = true
    }

    buildTypes {
        debug {
            // Enable optimizations while keeping debuggable
            isDebuggable = true
            isMinifyEnabled = true  // Enable R8 code shrinking
            isShrinkResources = true  // Remove unused resources
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "proguard-rules-debug.pro"  // Debug-specific rules
            )
        }
        release {
            isMinifyEnabled = true  // Enable for release too
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Enable core library desugaring for Java 17 features on older devices
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
    }

    // Optimize packaging
    packaging {
        resources {
            // Exclude unnecessary files to reduce APK size
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE.txt",
                "META-INF/ASL2.0"
            )
            // Pick first match for duplicate files
            pickFirsts += listOf(
                "META-INF/atomicfu.kotlin_module",
                "META-INF/kotlinx-coroutines-core.kotlin_module",
                "META-INF/kotlinx-coroutines-core-jvm.kotlin_module"
            )
        }
    }

    // Configure test options for faster tests
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true  // Faster tests
        }
        animationsDisabled = true  // Disable animations in tests
    }

    // Enable baseline profiles for faster app startup
    // This requires the macrobenchmark library to generate profiles
    // For now, we'll configure the build to support baseline profiles
    androidResources {
        noCompress += listOf("json", "txt", "html", "htm", "xml")  // Don't compress these
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Image loading
    implementation(libs.coil.compose)

    // Model Context Protocol (MCP) SDK
    implementation("io.modelcontextprotocol:kotlin-sdk:0.12.0")

    // Tink cryptography (required by MCP SDK at runtime)
    implementation("com.google.crypto.tink:tink-android:1.16.0")

    // Debug
    debugImplementation(libs.androidx.ui.tooling)

    // Core library desugaring for Java 17 features on older devices
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-Xcontext-receivers",
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${project.layout.buildDirectory.get().asFile.absolutePath}/compose_metrics"
        )
    }
}
