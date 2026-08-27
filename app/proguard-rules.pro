# ============================================================================
# MyFin Vault - ProGuard & R8 Optimization Rules
# ============================================================================

# 1. Room Persistence Library & Database Implementations
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase$Callback
-keep class * extends androidx.room.migration.Migration
-keep class * implements com.example.myfin.data.BudgetDao { *; }
-keep class com.example.myfin.data.AppDatabase_Impl { *; }
-keep class com.example.myfin.data.BudgetDao_Impl { *; }

-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.Insert *;
    @androidx.room.Update *;
    @androidx.room.Delete *;
    @androidx.room.Query *;
    @androidx.room.Transaction *;
}

# 2. Domain Entities, Enums & UI Data State
-keep class com.example.myfin.data.** { *; }
-keepclassmembers enum com.example.myfin.data.TransactionType { *; }
-keepclassmembers enum com.example.myfin.ui.components.NavigationTarget { *; }
-keepclassmembers class com.example.myfin.ui.MonthlyUiState { *; }
-keepclassmembers class com.example.myfin.ui.YearlyUiState { *; }
-keepclassmembers class com.example.myfin.ui.DashboardMetrics { *; }

# 3. AndroidX Security, MasterKey & Google Tink Crypto
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
-keepclassmembers class * extends java.lang.Enum {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 4. Apache POI & Excel Ledger Serialization (XSSFWorkbook)
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }
-keep class com.microsoft.schemas.office.** { *; }
-keep class com.microsoft.schemas.vml.** { *; }

-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.openxmlformats.**
-dontwarn net.sf.saxon.**
-dontwarn org.w3c.dom.bootstrap.**

-keepclassmembers class * {
    public static javax.xml.stream.XMLInputFactory newInstance();
    public static javax.xml.stream.XMLOutputFactory newInstance();
    public static javax.xml.stream.XMLEventFactory newInstance();
}

# 5. Biometric Prompt & Authentication
-keep class androidx.biometric.** { *; }

# 6. JSON Serialization (Backup & Restore Engines)
-keepclassmembers class org.json.** { *; }

# 7. Coroutines, Lifecycle & ViewModels
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# 8. Manifest Entry Points, Providers & Broadcast Receivers
-keep class androidx.core.content.FileProvider { *; }
-keep class com.example.myfin.data.NotificationReceiver {
    public <init>();
}
-keep class com.example.myfin.data.BootReceiver {
    public <init>();
}

# 9. Jetpack Compose Runtime
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
