# Lumen Gallery - Aggressive ProGuard/R8 Optimization Rules
# F-Droid & Lightweight APK configuration

# R8 Aggressive optimizations
-repackageclasses 'org.lumengallery.internal'
-allowaccessmodification
-mergeinterfacesaggressively

# Strip all android.util.Log calls in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# Kotlin standard optimizations
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNullParameter(...);
    public static void checkNotNull(...);
    public static void checkParameterIsNotNull(...);
}

# Room Database rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class * implements androidx.room.Entity
-keepclassmembers class * {
    @androidx.room.Dao *;
    @androidx.room.Entity *;
}

# Coil optimizations
-keep class coil.compose.** { *; }
-dontwarn coil.**

# Jetpack Compose rules
-dontwarn androidx.compose.**
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Navigation Compose rules
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# Media3 / ExoPlayer minimal rules
-keep class androidx.media3.exoplayer.** { *; }
-dontwarn androidx.media3.**

# Strip source file attributes and line numbers for maximum bytecode compactness
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
