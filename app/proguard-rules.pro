-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

-keep class com.streamvault.data.models.** { *; }
-keepclassmembers class com.streamvault.data.models.** { *; }

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn com.google.errorprone.**

-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

-keep class com.google.android.gms.cast.** { *; }
-dontwarn com.google.android.gms.**
