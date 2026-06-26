package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.CelApplication
import com.example.data.api.SpamApi
import com.example.data.database.Blacklist
import com.example.data.database.BlockedNumber
import com.example.data.database.CallLog
import com.example.data.database.SmsLog
import com.example.data.database.UserSettings
import com.example.data.database.Whitelist
import com.example.data.repository.SpamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CelViewModel(
    application: Application,
    private val repository: SpamRepository,
    private val spamApi: SpamApi
) : AndroidViewModel(application) {

    val callLogs: StateFlow<List<CallLog>> = repository.allCallLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val smsLogs: StateFlow<List<SmsLog>> = repository.allSmsLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val whitelist: StateFlow<List<Whitelist>> = repository.allWhitelist
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val blacklist: StateFlow<List<Blacklist>> = repository.allBlacklist
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val blockedNumbers: StateFlow<List<BlockedNumber>> = repository.allBlockedNumbers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userSettings: StateFlow<UserSettings?> = repository.userSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun updateLevel(level: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettings()
            repository.updateSettings(current.copy(level = level))
        }
    }

    fun toggleBlockUnknown(block: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettings()
            repository.updateSettings(current.copy(blockUnknown = block))
        }
    }

    fun toggleBlockSubsequent(block: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettings()
            repository.updateSettings(current.copy(blockSubsequent = block))
        }
    }

    fun updateCustomKeywords(keywords: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettings()
            repository.updateSettings(current.copy(customKeywords = keywords))
        }
    }

    fun addNumberToBlacklist(number: String, name: String, reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addBlacklist(number, name, reason)
        }
    }

    fun removeNumberFromBlacklist(number: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeBlacklist(number)
        }
    }

    fun addNumberToWhitelist(number: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addWhitelist(number, name)
        }
    }

    fun removeNumberFromWhitelist(number: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeWhitelist(number)
        }
    }

    fun reportSpam(number: String) {
        viewModelScope.launch(Dispatchers.IO) {
            spamApi.reportNumber(number)
        }
    }

    fun syncSpamDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            spamApi.syncDatabase()
        }
    }

    // Factory Provider
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val app = application as CelApplication
            return CelViewModel(application, app.repository, app.spamApi) as T
        }
    }
}
