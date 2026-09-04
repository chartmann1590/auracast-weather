# Keep Retrofit / kotlinx.serialization DTOs
-keep class com.auracast.weather.data.** { *; }
-keepclassmembers class com.auracast.weather.data.** { *; }

# Keep Room entities
-keep class androidx.room.** { *; }

# Hilt — generated code
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# kotlinx-serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Firebase / Play Billing consumer rules are bundled; no manual keeps needed beyond smoke test

# Google ML Kit Translate
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# LiteRT LM
-keep class com.google.ai.edge.litertlm.** { *; }
-dontwarn com.google.ai.edge.litertlm.**

# osmdroid
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**
