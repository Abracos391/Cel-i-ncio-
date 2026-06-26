package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_numbers")
data class BlockedNumber(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val number: String,
    val name: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "spam_reports")
data class SpamReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val number: String,
    val category: String,
    val comment: String,
    val count: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "call_logs")
data class CallLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val number: String,
    val name: String?,
    val spamScore: Int,
    val isBlocked: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val blockReason: String? = null
)

@Entity(tableName = "sms_logs")
data class SmsLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val number: String,
    val body: String,
    val spamScore: Int,
    val isBlocked: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val blockReason: String? = null
)

@Entity(tableName = "whitelist")
data class Whitelist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val number: String,
    val name: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "blacklist")
data class Blacklist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val number: String,
    val name: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1,
    val level: String = "Médio", // Leve, Médio, Agressivo, Modo Escudo
    val blockUnknown: Boolean = false,
    val blockSubsequent: Boolean = false,
    val customKeywords: String = "dívida,urgente,pagamento,negociação,proposta,atraso,crédito,empréstimo,oferta,promoção,pix,boleto",
    val countBlockedCalls: Int = 0,
    val countBlockedSms: Int = 0
)
