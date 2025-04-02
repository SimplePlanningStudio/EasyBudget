# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/benoit/Android/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keep class * extends androidx.fragment.app.Fragment {}
-dontwarn android.media.LoudnessCodecController$OnLoudnessCodecUpdateListener
-dontwarn android.media.LoudnessCodecController

# Retrofit and Gson ProGuard rules
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.simplebudget.** { *; }
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class com.google.gson.** { *; }
