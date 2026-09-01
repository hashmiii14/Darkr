# Darkr Production R8 Optimization Rules

# Keep ViewBinding generated classes
-keep class com.darkr.app.databinding.** { *; }

# Keep AndroidX Lifecycle and Service entry points
-keep class * extends android.app.Service
-keep class * extends android.app.Application
-keep class * extends android.app.Activity
-keep class * extends android.content.BroadcastReceiver

# Keep Kotlin Coroutines & Flow reflection metadata
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Optimization settings
-optimizationpasses 5
-dontpreverify
-repackageclasses
-allowaccessmodification

