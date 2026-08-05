# Keep AndroidX and WebKit classes
-keep class androidx.webkit.** { *; }

# Keep JS bridge interfaces
-keepclassmembers class fdb.r23studio.ai.** {
    @android.webkit.JavascriptInterface <methods>;
}
