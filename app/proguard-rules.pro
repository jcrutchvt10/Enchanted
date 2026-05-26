# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.enchanted.app.**$$serializer { *; }
-keepclassmembers class com.enchanted.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.enchanted.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Room entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Keep Tink classes and its service provider configuration files.
# Prevents R8/ProGuard from stripping required cryptographic implementations.
-keep class com.google.crypto.tink.** { *; }
-keepclassmembers class com.google.crypto.tink.** { *; }

# Tink references javax.lang.model.element.Modifier via errorprone annotations
# which is not available on Android — safe to ignore at runtime.
-dontwarn javax.lang.model.**
-dontwarn com.google.errorprone.**
