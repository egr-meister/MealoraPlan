# Mealora Plan ProGuard / R8 rules

# --- Kotlinx Serialization ---
# Keep the @Serializable annotation and generated serializer companions.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# Keep serialization core so reflectionless serializers resolve at runtime.
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all @Serializable model classes and their synthesized $serializer.
-keep,includedescriptorclasses class com.mealora.plan.**$$serializer { *; }
-keepclassmembers class com.mealora.plan.** {
    *** Companion;
}
-keepclasseswithmembers class com.mealora.plan.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep enum values used by serialization.
-keepclassmembers enum com.mealora.plan.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- Compose ---
-keep class androidx.compose.runtime.** { *; }

# --- General AndroidX / DataStore (handled by consumer rules, kept defensively) ---
-keep class androidx.datastore.*.** {*;}
