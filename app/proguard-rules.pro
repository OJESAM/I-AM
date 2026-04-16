# Firebase
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn com.google.firebase.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep @retrofit2.http.* class * { *; }
-dontwarn retrofit2.**

# Moshi
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.Json class *
-keep class * {
    @com.squareup.moshi.Json *;
}

# Keep Entity classes
-keep class com.example.kairoslivingstewards.data.local.entities.** { *; }
-keepclassmembers class com.example.kairoslivingstewards.data.local.entities.** { *; }
