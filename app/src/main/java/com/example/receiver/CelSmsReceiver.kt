package com.example.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import androidx.core.app.NotificationCompat
import com.example.CelApplication
import com.example.data.repository.SpamRepository
import com.example.domain.engine.SpamEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CelSmsReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return

        val app = context.applicationContext as CelApplication
        val repo = app.repository
        val engine = app.spamEngine

        val bundle = intent.extras ?: return
        try {
            val pdus = bundle.get("pdus") as? Array<*> ?: return
            val format = bundle.getString("format")

            for (pdu in pdus) {
                val sms = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    SmsMessage.createFromPdu(pdu as ByteArray, format)
                } else {
                    @Suppress("DEPRECATION")
                    SmsMessage.createFromPdu(pdu as ByteArray)
                }

                val number = sms.originatingAddress ?: ""
                val body = sms.messageBody ?: ""

                if (number.isBlank() || body.isBlank()) continue

                receiverScope.launch {
                    val evaluation = engine.evaluateSms(number, body, repo)

                    // Log in our local SQLite database
                    repo.logSms(
                        number = number,
                        body = body,
                        spamScore = evaluation.score,
                        isBlocked = evaluation.shouldBlock,
                        blockReason = if (evaluation.shouldBlock) evaluation.reasons.firstOrNull() else null
                    )

                    if (evaluation.shouldBlock) {
                        // Display notification about blocked SMS spam
                        showSmsBlockNotification(
                            context,
                            number,
                            body,
                            evaluation.score,
                            evaluation.reasons.firstOrNull() ?: "Conteúdo Proibido"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Safe fall-through
        }
    }

    private fun showSmsBlockNotification(
        context: Context,
        number: String,
        body: String,
        score: Int,
        reason: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, "block_channel_id")
            .setSmallIcon(android.R.drawable.stat_sys_warning) // Fallback system icon
            .setContentTitle("SMS Spam Bloqueado")
            .setContentText("De: $number | Risco: $score%")
            .setStyle(NotificationCompat.BigTextStyle().bigText("De: $number\nScore: $score%\nMotivo: $reason\n\nMensagem silenciada:\n\"$body\""))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(number.hashCode() + 1, builder.build())
    }
}
