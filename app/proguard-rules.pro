# MediaPipe Tasks Vision Keep Rules
-keep class com.google.mediapipe.tasks.vision.** { *; }
-keep class com.google.mediapipe.framework.image.** { *; }
-dontwarn com.google.mediapipe.**

# ARCore Keep Rules
-keep class com.google.ar.core.** { *; }
-dontwarn com.google.ar.core.**

# Room Keep Rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# Native Methods
-keepclasseswithmembernames class * {
    native <methods>;
}
