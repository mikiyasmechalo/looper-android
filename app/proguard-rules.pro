# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# jaudiotagger rules (Desktop Java classes not present on Android)
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.swing.**
-keep class org.jaudiotagger.** { *; }

# Media3 / Glance / WorkManager rules
-keep class androidx.glance.** { *; }
-keep interface androidx.glance.** { *; }
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    <init>();
    *;
}
-keep class com.example.looper_android.ui.widgets.** { *; }
-keep class androidx.work.** { *; }
-keep interface androidx.work.** { *; }
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.InputMerger { *; }
-keep class androidx.media3.** { *; }