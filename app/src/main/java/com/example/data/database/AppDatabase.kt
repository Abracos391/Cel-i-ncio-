package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BlockedNumber::class,
        SpamReport::class,
        CallLog::class,
        SmsLog::class,
        Whitelist::class,
        Blacklist::class,
        UserSettings::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockedNumberDao(): BlockedNumberDao
    abstract fun spamReportDao(): SpamReportDao
    abstract fun callLogDao(): CallLogDao
    abstract fun smsLogDao(): SmsLogDao
    abstract fun whitelistDao(): WhitelistDao
    abstract fun blacklistDao(): BlacklistDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "celiencio_database"
                )
                    .fallbackToDestructiveMigration() // safe default for simple templates
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
