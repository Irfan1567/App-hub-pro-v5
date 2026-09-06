# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class com.example.runtime.AppRuntimeBridge { *; }
-keep class * extends androidx.room.RoomDatabase
-keep class com.example.data.** { *; }
-keepclassmembers class com.example.runtime.** { *; }

-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
