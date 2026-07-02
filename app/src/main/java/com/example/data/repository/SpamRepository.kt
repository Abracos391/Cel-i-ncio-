package com.example.data.repository

import com.example.data.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class SpamRepository(private val db: AppDatabase) {

    private val blockedNumberDao = db.blockedNumberDao()
    private val spamReportDao = db.spamReportDao()
    private val callLogDao = db.callLogDao()
    private val smsLogDao = db.smsLogDao()
    private val whitelistDao = db.whitelistDao()
    private val blacklistDao = db.blacklistDao()
    private val userSettingsDao = db.userSettingsDao()

    val allBlockedNumbers: Flow<List<BlockedNumber>> = blockedNumberDao.getAllFlow()
    val allSpamReports: Flow<List<SpamReport>> = spamReportDao.getAllFlow()
    val allCallLogs: Flow<List<CallLog>> = callLogDao.getAllFlow()
    val allSmsLogs: Flow<List<SmsLog>> = smsLogDao.getAllFlow()
    val allWhitelist: Flow<List<Whitelist>> = whitelistDao.getAllFlow()
    val allBlacklist: Flow<List<Blacklist>> = blacklistDao.getAllFlow()
    val userSettings: Flow<UserSettings?> = userSettingsDao.getSettingsFlow()

    suspend fun getSettings(): UserSettings {
        var settings = userSettingsDao.getSettings()
        if (settings == null) {
            settings = UserSettings()
            userSettingsDao.insertOrUpdate(settings)
        }
        return settings
    }

    suspend fun updateSettings(settings: UserSettings) {
        userSettingsDao.insertOrUpdate(settings)
    }

    suspend fun addBlacklist(number: String, name: String, reason: String) {
        val normalized = normalizeNumber(number)
        blacklistDao.insert(Blacklist(number = normalized, name = name, reason = reason))
        blockedNumberDao.insert(BlockedNumber(number = normalized, name = name, reason = "Lista Negra: $reason"))
    }

    suspend fun removeBlacklist(number: String) {
        val normalized = normalizeNumber(number)
        blacklistDao.deleteByNumber(normalized)
        blockedNumberDao.deleteByNumber(normalized)
    }

    suspend fun addWhitelist(number: String, name: String) {
        val normalized = normalizeNumber(number)
        whitelistDao.insert(Whitelist(number = normalized, name = name))
        // Auto-remove from blacklist/blocked numbers if whitelisted
        blacklistDao.deleteByNumber(normalized)
        blockedNumberDao.deleteByNumber(normalized)
    }

    suspend fun removeWhitelist(number: String) {
        val normalized = normalizeNumber(number)
        whitelistDao.deleteByNumber(normalized)
    }

    suspend fun logCall(number: String, name: String?, spamScore: Int, isBlocked: Boolean, blockReason: String?) {
        callLogDao.insert(
            CallLog(
                number = number,
                name = name,
                spamScore = spamScore,
                isBlocked = isBlocked,
                blockReason = blockReason
            )
        )
        if (isBlocked) {
            incrementBlockedCounts(call = true)
            blockedNumberDao.insert(
                BlockedNumber(
                    number = number,
                    name = name ?: "Desconhecido",
                    reason = blockReason ?: "Spam Detectado"
                )
            )
        }
    }

    suspend fun logSms(number: String, body: String, spamScore: Int, isBlocked: Boolean, blockReason: String?) {
        smsLogDao.insert(
            SmsLog(
                number = number,
                body = body,
                spamScore = spamScore,
                isBlocked = isBlocked,
                blockReason = blockReason
            )
        )
        if (isBlocked) {
            incrementBlockedCounts(call = false)
            blockedNumberDao.insert(
                BlockedNumber(
                    number = number,
                    name = "Remetente SMS",
                    reason = "SMS: ${blockReason ?: "Spam Detectado"}"
                )
            )
        }
    }

    suspend fun isWhitelisted(number: String): Boolean {
        val normalized = normalizeNumber(number)
        return whitelistDao.getByNumber(normalized) != null
    }

    suspend fun isBlacklisted(number: String): Boolean {
        val normalized = normalizeNumber(number)
        return blacklistDao.getByNumber(normalized) != null
    }

    suspend fun isSpamReported(number: String): Boolean {
        val normalized = normalizeNumber(number)
        return spamReportDao.getByNumber(normalized) != null
    }

    suspend fun getRecentCallLogsCountForNumber(number: String, timeWindowMs: Long): Int {
        val logs = callLogDao.getLogsForNumber(number)
        val cutoff = System.currentTimeMillis() - timeWindowMs
        return logs.count { it.timestamp >= cutoff }
    }

    private suspend fun incrementBlockedCounts(call: Boolean) {
        val settings = getSettings()
        val updated = if (call) {
            settings.copy(countBlockedCalls = settings.countBlockedCalls + 1)
        } else {
            settings.copy(countBlockedSms = settings.countBlockedSms + 1)
        }
        userSettingsDao.insertOrUpdate(updated)
    }

    fun normalizeNumber(number: String): String {
        // Removes non-digit characters except maybe country code '+'
        var cleaned = number.replace(Regex("[^0-9+]"), "")
        // Normalize Brazilian phone numbers (e.g., removing +55 or trunk codes) for robust comparison
        if (cleaned.startsWith("+55")) {
            cleaned = cleaned.substring(3)
        }
        if (cleaned.startsWith("55") && cleaned.length >= 10) {
            cleaned = cleaned.substring(2)
        }
        // Remove leading 0
        if (cleaned.startsWith("0") && cleaned.length > 1) {
            cleaned = cleaned.substring(1)
        }
        return cleaned
    }
}
