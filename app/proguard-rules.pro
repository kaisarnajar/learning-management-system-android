# kotlinx.serialization: keep generated serializers of the API models.
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keepclassmembers @kotlinx.serialization.Serializable class com.darsequran.academy.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.darsequran.academy.**$$serializer { *; }
# Retrofit service interfaces are used through reflection.
-keep,allowobfuscation,allowshrinking interface com.darsequran.academy.data.remote.** { *; }
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
