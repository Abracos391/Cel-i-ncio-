package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedNumberDao {
    @Query("SELECT * FROM blocked_numbers ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<BlockedNumber>>

    @Query("SELECT * FROM blocked_numbers WHERE number = :number LIMIT 1")
    suspend fun getByNumber(number: String): BlockedNumber?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(blockedNumber: BlockedNumber)

    @Delete
    suspend fun delete(blockedNumber: BlockedNumber)

    @Query("DELETE FROM blocked_numbers WHERE number = :number")
    suspend fun deleteByNumber(number: String)
}

@Dao
interface SpamReportDao {
    @Query("SELECT * FROM spam_reports ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<SpamReport>>

    @Query("SELECT * FROM spam_reports WHERE number = :number LIMIT 1")
    suspend fun getByNumber(number: String): SpamReport?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(spamReport: SpamReport)

    @Delete
    suspend fun delete(spamReport: SpamReport)
}

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<CallLog>>

    @Query("SELECT * FROM call_logs WHERE number = :number ORDER BY timestamp DESC")
    suspend fun getLogsForNumber(number: String): List<CallLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(callLog: CallLog)

    @Query("DELETE FROM call_logs")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM call_logs WHERE isBlocked = 1")
    fun getBlockedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM call_logs WHERE isBlocked = 1")
    suspend fun getBlockedCount(): Int
}

@Dao
interface SmsLogDao {
    @Query("SELECT * FROM sms_logs ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<SmsLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(smsLog: SmsLog)

    @Query("DELETE FROM sms_logs")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM sms_logs WHERE isBlocked = 1")
    fun getBlockedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sms_logs WHERE isBlocked = 1")
    suspend fun getBlockedCount(): Int
}

@Dao
interface WhitelistDao {
    @Query("SELECT * FROM whitelist ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<Whitelist>>

    @Query("SELECT * FROM whitelist WHERE number = :number LIMIT 1")
    suspend fun getByNumber(number: String): Whitelist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(whitelist: Whitelist)

    @Delete
    suspend fun delete(whitelist: Whitelist)

    @Query("DELETE FROM whitelist WHERE number = :number")
    suspend fun deleteByNumber(number: String)
}

@Dao
interface BlacklistDao {
    @Query("SELECT * FROM blacklist ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<Blacklist>>

    @Query("SELECT * FROM blacklist WHERE number = :number LIMIT 1")
    suspend fun getByNumber(number: String): Blacklist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(blacklist: Blacklist)

    @Delete
    suspend fun delete(blacklist: Blacklist)

    @Query("DELETE FROM blacklist WHERE number = :number")
    suspend fun deleteByNumber(number: String)
}

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<UserSettings?>

    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): UserSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: UserSettings)
}
