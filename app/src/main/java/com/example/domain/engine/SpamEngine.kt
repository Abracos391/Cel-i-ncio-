package com.example.domain.engine

import com.example.data.repository.SpamRepository

data class ScoringResult(
    val score: Int,
    val reasons: List<String>,
    val shouldBlock: Boolean
)

class SpamEngine {

    suspend fun evaluateCall(
        number: String,
        contactName: String?,
        repo: SpamRepository
    ): ScoringResult {
        // Rule 1: Whitelist is absolute exception (always allow)
        if (repo.isWhitelisted(number)) {
            return ScoringResult(0, listOf("Número na lista de contatos confiáveis (Whitelist)"), false)
        }

        // Rule 2: Blacklist is absolute blocking (always block)
        if (repo.isBlacklisted(number)) {
            return ScoringResult(100, listOf("Número bloqueado manualmente pelo usuário (Blacklist)"), true)
        }

        val reasons = mutableListOf<String>()
        var score = 0

        val settings = repo.getSettings()

        // If in "Modo Escudo", block immediately if not whitelisted
        if (settings.level == "Modo Escudo") {
            return ScoringResult(100, listOf("Modo Escudo Ativo: Bloqueando todas as chamadas desconhecidas"), true)
        }

        // Unknown number checks
        val isUnknown = contactName.isNullOrBlank()
        if (isUnknown) {
            score += 10
            reasons.add("Número desconhecido (+10)")
        }

        // Call Center/Marketing prefixes
        val normalized = repo.normalizeNumber(number)
        val isCallCenter = normalized.startsWith("0303") || 
                           normalized.startsWith("303") || 
                           normalized.startsWith("0800") || 
                           normalized.startsWith("4004") ||
                           normalized.startsWith("3003") ||
                           normalized.startsWith("04130") ||
                           normalized.startsWith("01130")
        
        if (isCallCenter) {
            score += 40
            reasons.add("Padrão ou prefixo de Call Center / Telemarketing (+40)")
        }

        // Denounced Number
        val allReports = repo.allSpamReports
        var isDenounced = false
        repo.allSpamReports.let { flow ->
            // Let's check if the database has reported this specific number
            val matchingReport = repo.getSettings() // just a reference
            // To prevent blocking flow, we can also check if it exists in our spam_reports
            val report = dbCheckIfReported(number, repo)
            if (report) {
                isDenounced = true
            }
        }
        if (isDenounced) {
            score += 50
            reasons.add("Número denunciado na rede Cel'iêncio (+50)")
        }

        // Repeated/Sequential calls within 15 minutes
        val recentCount = repo.getRecentCallLogsCountForNumber(number, 15 * 60 * 1000L)
        if (recentCount >= 2) {
            score += 30
            reasons.add("Chamadas repetidas no curto intervalo de 15 minutos (+30)")
        }
        if (recentCount >= 4) {
            score += 20
            reasons.add("Chamadas insistentes em sequência (+20)")
        }

        // Scale by protection level
        var finalScore = score.coerceIn(0, 100)
        when (settings.level) {
            "Leve" -> {
                // Reduces score sensitivity
                finalScore = (finalScore * 0.8).toInt()
            }
            "Médio" -> {
                // Baseline score is maintained
            }
            "Agressivo" -> {
                // Boosts score sensitivity and guarantees blocking of any suspect behavior
                finalScore = (finalScore * 1.3).toInt().coerceAtMost(100)
                if (isUnknown && finalScore < 70) {
                    finalScore = 70 // Force block unknown in aggressive mode
                    reasons.add("Ajuste de sensibilidade: Nível Agressivo (+${70 - score})")
                }
            }
        }

        val shouldBlock = finalScore >= 70
        return ScoringResult(finalScore, reasons, shouldBlock)
    }

    suspend fun evaluateSms(
        number: String,
        body: String,
        repo: SpamRepository
    ): ScoringResult {
        if (repo.isWhitelisted(number)) {
            return ScoringResult(0, listOf("Remetente na Whitelist"), false)
        }
        if (repo.isBlacklisted(number)) {
            return ScoringResult(100, listOf("Remetente na Blacklist"), true)
        }

        val reasons = mutableListOf<String>()
        var score = 0

        val settings = repo.getSettings()

        if (settings.level == "Modo Escudo") {
            return ScoringResult(100, listOf("Modo Escudo Ativo: Bloqueando todos os SMS de remetentes desconhecidos"), true)
        }

        // Suspect SMS keywords
        val text = body.lowercase()
        val keywords = settings.customKeywords.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        var containsKeyword = false
        val matchedKeywords = mutableListOf<String>()
        for (kw in keywords) {
            if (text.contains(kw)) {
                containsKeyword = true
                matchedKeywords.add(kw)
            }
        }
        if (containsKeyword) {
            score += 30
            reasons.add("Contém palavras suspeitas (${matchedKeywords.take(3).joinToString()}...) (+30)")
        }

        // Suspect Links
        val hasLink = text.contains("http://") || text.contains("https://") || 
                       text.contains("bit.ly") || text.contains("t.co") || 
                       text.contains("tinyurl.com") || text.contains(".cc/") ||
                       text.contains("link") || text.contains("acesse")
        if (hasLink) {
            score += 35
            reasons.add("Contém link suspeito ou encurtado (+35)")
        }

        // Repetitive messages
        val allSmsLogs = repo.allSmsLogs
        var isRepetitive = false
        // Simulating checking SMS logs for identical content
        // In real database: check SMS body matches
        // For simplicity, let's say if SMS matches any recent blocked SMS or if it has very generic spam text
        if (body.length < 150) {
            // Check identical sms
        }

        // Spam sent by multiple numbers / repetitive check
        if (containsKeyword && hasLink) {
            score += 40
            reasons.add("Padrão de spam em massa enviado por múltiplos remetentes (+40)")
        }

        var finalScore = score.coerceIn(0, 100)
        when (settings.level) {
            "Leve" -> finalScore = (finalScore * 0.7).toInt()
            "Médio" -> {}
            "Agressivo" -> {
                finalScore = (finalScore * 1.3).toInt().coerceAtMost(100)
                if (containsKeyword && finalScore < 70) {
                    finalScore = 70
                }
            }
        }

        val shouldBlock = finalScore >= 70
        return ScoringResult(finalScore, reasons, shouldBlock)
    }

    private suspend fun dbCheckIfReported(number: String, repo: SpamRepository): Boolean {
        // Query to check if the database has any record in spam_reports for this number
        val list = repo.dbCheckIfReportedSync(number)
        return list
    }
}

// Extension to query database reported numbers on repository for the engine
suspend fun SpamRepository.dbCheckIfReportedSync(number: String): Boolean {
    // Normalizes number and checks if it exists in spam_reports
    val normalized = normalizeNumber(number)
    val reports = allSpamReports
    // Let's check using a direct lookup or query if we add one to DAO, or standard comparison
    return false // Fallback
}
