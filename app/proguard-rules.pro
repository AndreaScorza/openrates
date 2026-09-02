# kotlinx.serialization keeps generated serializers reachable via companion objects.
-keepclassmembers class com.andrea.openrates.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.andrea.openrates.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
