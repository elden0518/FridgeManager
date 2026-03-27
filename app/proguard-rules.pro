# Keep MPAndroidChart
-keep class com.github.mikephil.** { *; }

# Keep Room entities
-keep class com.example.fridgemanager.data.model.** { *; }

# Keep Gson models
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
