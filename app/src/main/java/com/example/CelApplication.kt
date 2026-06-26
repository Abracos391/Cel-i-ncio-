package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.api.LocalSpamApiImpl
import com.example.data.api.SpamApi
import com.example.data.database.AppDatabase
import com.example.data.repository.SpamRepository
import com.example.domain.engine.SpamEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CelApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { SpamRepository(database) }
    val spamApi by lazy { LocalSpamApiImpl(this, database) }
    val spamEngine by lazy { SpamEngine() }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        
        // Populate sample spam list and settings on first launch
        applicationScope.launch {
            spamApi.syncDatabase()
            repository.getSettings() // Ensures user settings row id = 1 is created
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Avisos de Bloqueio"
            val descriptionText = "Notificações exibidas ao bloquear chamadas e SMS indesejados"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("block_channel_id", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
