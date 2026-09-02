# ============================================================================
# MyFin Vault - Hardened Production ProGuard & R8 Optimization Rules
# ============================================================================

# ----------------------------------------------------------------------------
# 1. Room Persistence & SQLite Storage Engine
# ----------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase$Callback
-keep class * extends androidx.room.migration.Migration
-keep class **_Impl { *; }

-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.PrimaryKey *;
    @androidx.room.ColumnInfo *;
    @androidx.room.Embedded *;
    @androidx.room.Relation *;
    @androidx.room.Insert *;
    @androidx.room.Update *;
    @androidx.room.Delete *;
    @androidx.room.Query *;
    @androidx.room.Transaction *;
}
-dontwarn androidx.room.paging.**

# ----------------------------------------------------------------------------
# 2. Domain Data Classes & Offline JSON Serialization Engine
# ----------------------------------------------------------------------------
# Protect database entities and backup schema from field renaming
-keep class com.example.myfin.data.** { *; }
-keepclassmembers class com.example.myfin.data.** { *; }

# Preserve UI State classes used across ViewModels and Compose states
-keepclassmembers class com.example.myfin.ui.MonthlyUiState { *; }
-keepclassmembers class com.example.myfin.ui.YearlyUiState { *; }
-keepclassmembers class com.example.myfin.ui.DashboardMetrics { *; }
-keepclassmembers class com.example.myfin.ui.CategoryPerformance { *; }

# Universal Enum Preservation (Supports values(), valueOf(), and Kotlin .entries)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public static ** getEntries();
    public static ** entries();
    **[] $VALUES;
    ** $ENTRIES;
}
-keep enum com.example.myfin.data.** { *; }
-keep enum com.example.myfin.ui.components.NavigationTarget { *; }
-keep enum com.example.myfin.ui.components.SettingsActiveSheet { *; }
-keep enum com.example.myfin.ui.screens.VaultTier { *; }
-keep enum com.example.myfin.ui.screens.TimeRangeFilter { *; }
-keep enum com.example.myfin.ui.screens.VelocityRange { *; }
-keep enum com.example.myfin.ui.screens.SettingsAccordionSection { *; }
-keep enum com.example.myfin.ui.screens.GuideAccordionSection { *; }

# ----------------------------------------------------------------------------
# 3. AndroidX Security, MasterKey, Biometrics & Hardware KeyStore
# ----------------------------------------------------------------------------
-keep class androidx.security.crypto.** { *; }
-keep class androidx.biometric.** { *; }
-keep interface androidx.biometric.** { *; }
-keep class com.google.crypto.tink.** { *; }
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }
-dontwarn com.google.crypto.tink.**

# ----------------------------------------------------------------------------
# 4. Coil Image Loading Architecture (Avatar & Profile Rendering)
# ----------------------------------------------------------------------------
-keep class coil.** { *; }
-keepclassmembers class * implements coil.decode.Decoder$Factory { *; }
-keepclassmembers class * implements coil.fetch.Fetcher$Factory { *; }
-dontwarn coil.**

# ----------------------------------------------------------------------------
# 5. Apache POI & Excel Ledger Serialization (XSSFWorkbook)
# ----------------------------------------------------------------------------
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }
-keep class com.microsoft.schemas.office.** { *; }
-keep class com.microsoft.schemas.vml.** { *; }
-keep class com.example.myfin.data.ExcelExportManager { *; }

# Suppress missing platform references not present in Android runtime
-dontwarn java.awt.**
-dontwarn javax.xml.stream.**
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.openxmlformats.**
-dontwarn net.sf.saxon.**
-dontwarn org.w3c.dom.**
-dontwarn org.w3c.dom.bootstrap.**

-keepclassmembers class * {
    public static javax.xml.stream.XMLInputFactory newInstance();
    public static javax.xml.stream.XMLOutputFactory newInstance();
    public static javax.xml.stream.XMLEventFactory newInstance();
}

# ----------------------------------------------------------------------------
# 6. JSON Engine (Encrypted Vault Backup & Restore)
# ----------------------------------------------------------------------------
-keepclassmembers class org.json.** { *; }

# ----------------------------------------------------------------------------
# 7. Kotlin Coroutines & Lifecycle Architecture
# ----------------------------------------------------------------------------
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ----------------------------------------------------------------------------
# 8. Core Receivers, Alarms & FileProvider Access
# ----------------------------------------------------------------------------
-keep class androidx.core.content.FileProvider { *; }
-keep class com.example.myfin.data.ReminderScheduler { *; }
-keep class com.example.myfin.data.NotificationReceiver {
    public <init>();
}
-keep class com.example.myfin.data.BootReceiver {
    public <init>();
}

# ----------------------------------------------------------------------------
# 9. Jetpack Compose Framework Rules
# ----------------------------------------------------------------------------
-keepclassmembers class * extends androidx.compose.ui.Modifier { *; }
-dontwarn androidx.compose.animation.**
-dontwarn androidx.compose.material3.**

# ----------------------------------------------------------------------------
# 10. Strip Debug Telemetry in Release Artifacts
# ----------------------------------------------------------------------------
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
