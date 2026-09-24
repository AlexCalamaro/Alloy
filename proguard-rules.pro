# Alloy ProGuard/R8 Configuration

# Hilt
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.providers.HiltComponents { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep class com.squidink.alloy.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# DataStore
-keep class androidx.datastore.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# SQLCipher
-keep class net.zetetic.** { *; }
-dontwarn net.zetetic.**

# Compose
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.runtime.internal.ComposableKt {
    *;
}

# General
-dontwarn android.**
-dontwarn androidx.**
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
