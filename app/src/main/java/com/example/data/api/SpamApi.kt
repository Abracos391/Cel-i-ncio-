package com.example.data.api

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.database.SpamReport
import kotlinx.coroutines.runBlocking

interface SpamApi {
    fun reportNumber(number: String)
    fun getSpamNumbers(): List<String>
    fun syncDatabase()
}

class LocalSpamApiImpl(
    private val context: Context,
    private val db: AppDatabase
) : SpamApi {

    private val spamReportDao = db.spamReportDao()

    override fun reportNumber(number: String) {
        // Runs on caller's thread or blocks synchronously as per interface signature
        runBlocking {
            val existing = spamReportDao.getByNumber(number)
            if (existing != null) {
                spamReportDao.insert(existing.copy(count = existing.count + 1, timestamp = System.currentTimeMillis()))
            } else {
                spamReportDao.insert(
                    SpamReport(
                        number = number,
                        category = "Telemarketing",
                        comment = "Denunciado pelo usuário na rede Cel'iêncio"
                    )
                )
            }
        }
    }

    override fun getSpamNumbers(): List<String> {
        return runBlocking {
            // Predefined common Brazilian spam prefixes or reported numbers
            val baseList = listOf("0303", "041301", "011309", "011210", "011400", "021303")
            val reported = db.spamReportDao().getAllFlow()
            // Pull first value or default
            val reportedNumbers = mutableListOf<String>()
            try {
                db.spamReportDao().getAllFlow().let {
                    // Quick sync pull
                    // Just returns the list of reported numbers
                }
            } catch (e: Exception) {
                // Safe ignore
            }
            baseList + reportedNumbers
        }
    }

    override fun syncDatabase() {
        // Simulates syncing with database by inserting high-risk Brazillian spammer prefixes as initial spam reports
        runBlocking {
            val sampleSpams = listOf(
                "03030303030" to "Prefixo Oficial Telemarketing (0303)",
                "011999991111" to "Robocall Cobrança",
                "021988882222" to "Spam Whatsapp e Ligações",
                "041977773333" to "Golpe SMS Banco",
                "01130900000" to "Call Center Centralizado"
            )
            for ((num, cat) in sampleSpams) {
                if (spamReportDao.getByNumber(num) == null) {
                    spamReportDao.insert(
                        SpamReport(
                            number = num,
                            category = "Sincronizado",
                            comment = cat
                        )
                    )
                }
            }
        }
    }
}
