# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /sdk/tools/proguard/proguard-android.txt

# Keep data classes
-keepclassmembers class com.squidink.alloy.** {
    <init>(...);
}

# SQLCipher
-keep class net.sqlcipher.database.* { *; }
-dontwarn net.sqlcipher.database.**
