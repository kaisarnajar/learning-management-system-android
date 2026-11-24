# kotlinx.serialization: keep the generated serializers of the API models (data/remote, data/staff/**) and the routes.
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keepclassmembers @kotlinx.serialization.Serializable class com.darsequran.academy.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.darsequran.academy.**$$serializer { *; }
# Retrofit service interfaces (and suspend functions' generic signatures) are kept by Retrofit's own consumer rules.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
