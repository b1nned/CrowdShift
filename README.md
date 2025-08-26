# CrowdShift WebView App ProGuard Rules

# Keep all WebView related classes
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}

-keepclassmembers class * extends android.webkit.WebChromeClient {
    public void *(android.webkit.WebView, java.lang.String);
}

# Keep JavaScript interfaces
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep WebView classes
-keep public class android.webkit.WebView
-keep public class android.webkit.WebViewClient
-keep public class android.webkit.WebChromeClient
-keep public class android.webkit.WebSettings
-keep public class android.webkit.ValueCallback

# Keep Activity classes
-keep public class com.example.crowdshift.MainActivity
-keep public class com.example.crowdshift.SplashActivity

# Keep utility classes
-keep class com.example.crowdshift.utils.** { *; }

# Keep WebView client inner classes
-keep class com.example.crowdshift.MainActivity$CrowdShiftWebViewClient { *; }
-keep class com.example.crowdshift.MainActivity$CrowdShiftWebChromeClient { *; }

# Preserve annotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable

# Keep serializable classes
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# AndroidX and support library rules
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# Keep Material Design components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# Network security configuration
-keep class android.security.NetworkSecurityPolicy {
    *;
}

# Keep location and camera related classes
-keep class android.location.** { *; }
-keep class android.hardware.camera2.** { *; }

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Kotlin specific rules
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Keep coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep R class
-keep class **.R
-keep class **.R$* {
    <fields>;
}

# Keep BuildConfig
-keep class com.example.crowdshift.BuildConfig { *; }

# Optimization settings
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
