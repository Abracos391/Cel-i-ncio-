package com.example.service

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.CelApplication
import com.example.data.repository.SpamRepository
import com.example.domain.engine.SpamEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
class CelCallScreeningService : CallScreeningService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onScreenCall(callDetails: Call.Details) {
        val handle = callDetails.handle
        val number = handle?.schemeSpecificPart ?: ""
        
        if (number.isBlank()) {
            respondWith(callDetails, allow = true)
            return
        }

        val app = application as CelApplication
        val repo = app.repository
        val engine = app.spamEngine

        serviceScope.launch {
            try {
                // Determine contact name if exists (using Whitelist or we can do a mock query for Brazil contacts if needed)
                var contactName: String? = null
                val isWhitelisted = repo.isWhitelisted(number)
                if (isWhitelisted) {
                    contactName = "Contato Whitelist"
                }

                val evaluation = engine.evaluateCall(number, contactName, repo)

                // Log the call attempt in our Room database
                repo.logCall(
                    number = number,
                    name = contactName,
                    spamScore = evaluation.score,
                    isBlocked = evaluation.shouldBlock,
                    blockReason = if (evaluation.shouldBlock) evaluation.reasons.firstOrNull() else null
                )

                if (evaluation.shouldBlock) {
                    showBlockNotification(number, evaluation.score, evaluation.reasons.firstOrNull() ?: "Bloqueado pelo Escudo")
                    respondWith(callDetails, allow = false)
                } else {
                    respondWith(callDetails, allow = true)
                }
            } catch (e: Exception) {
                // If anything crashes, fail-safe to allow the call to make sure critical calls are never lost
                respondWith(callDetails, allow = true)
            }
        }
    }

    private fun respondWith(callDetails: Call.Details, allow: Boolean) {
        val response = if (!allow) {
            CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(true)
                .build()
        } else {
            CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
        }
        respondToCall(callDetails, response)
    }

    private fun showBlockNotification(number: String, score: Int, reason: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(this, "block_channel_id")
            .setSmallIcon(android.R.drawable.ic_secure) // fallback system icon
            .setContentTitle("Chamada Suspeita Silenciada")
            .setContentText("Número: $number | Spam Score: $score%\nMotivo: $reason")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Número: $number\nScore de Risco: $score%\nMotivo: $reason\n\nCel'iêncio protegeu seu foco e tranquilidade!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(number.hashCode(), builder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
