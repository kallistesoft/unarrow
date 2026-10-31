# Libraries used here (Hilt, kotlinx.serialization, Navigation 3, Play Billing, AdMob, Play In-App Updates and Review) ship their own
# consumer rules. Keep line numbers for readable crash reports in Play Console.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Play Review's Kotlin extensions reference an annotation from play-services-basement that isn't on the classpath
# (compile-time only, nothing to keep).
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite

# WorkManager (pulled in by the Google Mobile Ads SDK) opens its Room database by reflection on WorkDatabase_Impl; without
# these, R8 strips it and the release app crashes at startup.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
