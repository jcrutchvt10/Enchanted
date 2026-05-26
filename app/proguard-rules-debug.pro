# Debug-specific ProGuard rules
# Enable optimizations while preserving debuggability

# Keep debug information
-keepattributes SourceFile,LineNumberTable,*Annotation*

# Keep all classes and members that are accessed via reflection during debugging
-keep class androidx.lifecycle.** { *; }
-keep class androidx.compose.** { *; }
-keep class kotlin.coroutines.** { *; }

# Keep Hilt classes for debugging
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Room debugging infrastructure
-keep class androidx.room.** { *; }

# Keep serialization for debugging
-keep class kotlinx.serialization.** { *; }

# Keep Compose metadata for debugging
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class androidx.compose.** {
    *** Companion;
}

# Keep test-related classes if present
-keep class androidx.test.** { *; }
-keep class org.junit.** { *; }

# Optimize but keep debug symbols
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Enable optimization while keeping essential classes
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-allowaccessmodification

# Keep native method names for debugging
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enum values for debugging
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable and Serializable for debugging
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
-keep class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Reduce obfuscation for better debugging (keep class names but can obfuscate members)
-repackageclasses ''
-flattenpackagehierarchy
-allowaccessmodification

# Keep public API classes readable
-keep public class * {
    public protected *;
}

# Suppress warnings for missing Java SE classes (desugaring covers these at runtime)
-dontwarn com.sun.nio.file.SensitivityWatchEventModifier
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean