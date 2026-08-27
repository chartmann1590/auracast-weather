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
