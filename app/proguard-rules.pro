# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/matthew/android-studio/sdk/tools/proguard/proguard-android.txt
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

-dontwarn org.androidannotations.**
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses

-keepattributes SourceFile,LineNumberTable

-dontwarn org.apache.http.**
-dontwarn android.net.http.**
-dontwarn org.spongycastle.**
-keepnames class org.apache.** {*;}
-keep public class org.apache.** {*;}
-keep public class android.net.http.** {*;}
-keep class android.net.http.** {*;}
-keep class org.apache.** { *; }
-keep class org.spongycastle.**

-keepclassmembers class ** {
    public void onEvent*(**);
}