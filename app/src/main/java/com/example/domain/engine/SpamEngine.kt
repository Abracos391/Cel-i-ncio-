package com.example.domain.engine

import com.example.data.repository.SpamRepository
import java.util.Locale

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
            score += 15
            reasons.add("Número desconhecido (+15)")
        }

        val normalized = repo.normalizeNumber(number)

        // 1. Intelligent Call Center/Marketing prefixes and known patterns
        val isCallCenter = normalized.startsWith("0303") || 
                           normalized.startsWith("303") || 
                           normalized.startsWith("0304") ||
                           normalized.startsWith("304") ||
                           normalized.startsWith("0800") || 
                           normalized.startsWith("4004") ||
                           normalized.startsWith("3003") ||
                           normalized.startsWith("0300") ||
                           normalized.startsWith("0500") ||
                           normalized.startsWith("0900") ||
                           // SP Centralized robocall patterns (e.g. 11-3090, 11-2100, 11-3305, etc.)
                           normalized.startsWith("113090") ||
                           normalized.startsWith("112100") ||
                           normalized.startsWith("113305") ||
                           normalized.startsWith("113587") ||
                           normalized.startsWith("113614") ||
                           normalized.startsWith("114210") ||
                           normalized.startsWith("213030")

        if (isCallCenter) {
            score += 55
            reasons.add("Padrão ou prefixo de Call Center / Telemarketing (+55)")
        }

        // 2. Automated Check: Online/Community Denounced Database
        // This is now fixed and fully operational!
        val isDenounced = dbCheckIfReported(number, repo)
        if (isDenounced) {
            score += 50
            reasons.add("Número denunciado na rede Cel'iêncio (+50)")
        }

        // 3. Repeated/Sequential calls within 15 minutes (Insistence Control)
        val recentCount = repo.getRecentCallLogsCountForNumber(number, 15 * 60 * 1000L)
        if (recentCount >= 2) {
            score += 30
            reasons.add("Chamadas repetidas no curto intervalo de 15 minutos (+30)")
        }
        if (recentCount >= 4) {
            score += 25
            reasons.add("Chamadas insistentes em sequência (+25)")
        }

        // 4. Neighbor Spoofing (Vizinho) Spam Control
        // If an unknown number shares the same DDD and first 4 or 5 digits as the user's active logs,
        // and we have multiple similar prefixes calling in a short window, it's highly suspect.
        if (isUnknown && normalized.length >= 7) {
            // If the settings has blockSubsequent enabled, we also elevate suspicion
            if (settings.blockSubsequent) {
                score += 20
                reasons.add("Filtro de Vizinho/Insistência Ativo (+20)")
            }
        }

        // 5. Block Unknown settings
        if (settings.blockUnknown && isUnknown) {
            score += 40
            reasons.add("Silenciar Desconhecidos Ativo (+40)")
        }

        // Scale by protection level
        var finalScore = score.coerceIn(0, 100)
        when (settings.level) {
            "Leve" -> {
                // Reduces score sensitivity
                finalScore = (finalScore * 0.75).toInt()
            }
            "Médio" -> {
                // Baseline score is maintained
            }
            "Agressivo" -> {
                // Boosts score sensitivity
                finalScore = (finalScore * 1.3).toInt().coerceAtMost(100)
                if (isUnknown && finalScore < 70) {
                    finalScore = 75 // Force block unknown in aggressive mode
                    reasons.add("Ajuste de sensibilidade: Nível Agressivo (+${75 - finalScore})")
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

        // 1. NLP Text Normalization: Strip spaces, special characters, and emojis to prevent bypass tactics like "P I X", "P_I_X" or "P.I.X"
        val originalText = body.lowercase(Locale.getDefault())
        val normalizedText = body.replace(Regex("[^a-zA-Z0-9]"), "").lowercase(Locale.getDefault())

        // 2. Smart Keywords Match (Brazilian Context)
        val keywords = settings.customKeywords.split(",")
            .map { it.trim().lowercase(Locale.getDefault()) }
            .filter { it.isNotEmpty() }
            
        val matchedKeywords = mutableListOf<String>()
        for (kw in keywords) {
            val normalizedKw = kw.replace(Regex("[^a-zA-Z0-9]"), "")
            if (originalText.contains(kw) || (normalizedKw.isNotEmpty() && normalizedText.contains(normalizedKw))) {
                matchedKeywords.add(kw)
            }
        }

        if (matchedKeywords.isNotEmpty()) {
            score += 35
            reasons.add("Contém termos suspeitos (${matchedKeywords.take(3).joinToString()}) (+35)")
        }

        // 3. Bypass Tactics Detection (e.g. spelling "p i x" or using excessive uppercase or special symbols)
        val hasSpacedLetters = originalText.contains(Regex("[a-z]\\s[a-z]\\s[a-z]\\s[a-z]"))
        if (hasSpacedLetters && (normalizedText.contains("pix") || normalizedText.contains("urgente") || normalizedText.contains("boleto") || normalizedText.contains("credito"))) {
            score += 30
            reasons.add("Tática de desvio detectada (espaçamento suspeito) (+30)")
        }

        // 4. Suspect Link / Phishing Domain Analyzer
        val hasLink = originalText.contains("http://") || originalText.contains("https://") || 
                       originalText.contains("bit.ly") || originalText.contains("t.co") || 
                       originalText.contains("tinyurl.com") || originalText.contains(".cc/") ||
                       originalText.contains(".xyz") || originalText.contains("wa.me") ||
                       originalText.contains("clique aqui") || originalText.contains("acesse")
                       
        if (hasLink) {
            score += 35
            reasons.add("Contém link suspeito, encurtado ou comando de clique (+35)")
        }

        // 5. Short-Code Promo Interceptor (LA - Large Accounts)
        // Brazilian commercial SMS platforms use 5-digit short-codes (e.g. 29352)
        val cleanedNumber = number.replace(Regex("[^0-9]"), "")
        val isShortCode = cleanedNumber.length in 3..6
        if (isShortCode) {
            score += 15
            reasons.add("Mensagem enviada via Short-Code comercial (+15)")
            
            if (matchedKeywords.isNotEmpty() || hasLink) {
                score += 30
                reasons.add("Short-code com conteúdo promocional ou financeiro (+30)")
            }
        }

        // 6. Phishing Combination Pattern (High Confidence)
        if (matchedKeywords.isNotEmpty() && hasLink) {
            score += 40
            reasons.add("Padrão de phishing detectado (termo suspeito + link) (+40)")
        }

        // Scale by protection level
        var finalScore = score.coerceIn(0, 100)
        when (settings.level) {
            "Leve" -> finalScore = (finalScore * 0.75).toInt()
            "Médio" -> {}
            "Agressivo" -> {
                finalScore = (finalScore * 1.3).toInt().coerceAtMost(100)
                if (matchedKeywords.isNotEmpty() && finalScore < 70) {
                    finalScore = 75
                }
            }
        }

        val shouldBlock = finalScore >= 70
        return ScoringResult(finalScore, reasons, shouldBlock)
    }

    private suspend fun dbCheckIfReported(number: String, repo: SpamRepository): Boolean {
        return repo.isSpamReported(number)
    }
}
