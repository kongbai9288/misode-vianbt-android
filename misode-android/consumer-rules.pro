# Misode mobile (Android WebView wrapper)

# Methods called from JavaScript must keep their names.
-keepclassmembers class io.misode.mobile.MisodeView$NativeHost {
    @android.webkit.JavascriptInterface <methods>;
}

# Public API of the library.
-keep public class io.misode.mobile.** { public *; }

# The web bundle must survive resource shrinking untouched.
-keepresources assets/misode/**
