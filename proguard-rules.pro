# Alloy ProGuard/R8 Configuration

# Hilt & Dependency Injection
-keep class * extends dagger.hilt.android.internal.providers.HiltComponents { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# SQLCipher JNI bindings
-keep class net.zetetic.** { *; }
-dontwarn net.zetetic.**

# General
-dontwarn android.**
-dontwarn androidx.**
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
