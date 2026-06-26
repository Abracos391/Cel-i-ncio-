package com.example.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.CallLog
import com.example.data.database.SmsLog
import com.example.ui.viewmodel.CelViewModel
import java.util.*

@Composable
fun BlockedLogsScreen(viewModel: CelViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Chamadas", "SMS Spam")

    val callLogs by viewModel.callLogs.collectAsState()
    val smsLogs by viewModel.smsLogs.collectAsState()

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Registros de Bloqueio", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> CallLogsList(
                    logs = callLogs,
                    onAddToWhitelist = { viewModel.addNumberToWhitelist(it, "Contato Confiável") },
                    onReport = { viewModel.reportSpam(it) }
                )
                1 -> SmsLogsList(
                    logs = smsLogs,
                    onAddToWhitelist = { viewModel.addNumberToWhitelist(it, "Remetente Confiável") },
                    onReport = { viewModel.reportSpam(it) }
                )
            }
        }
    }
}

@Composable
fun CallLogsList(
    logs: List<CallLog>,
    onAddToWhitelist: (String) -> Unit,
    onReport: (String) -> Unit
) {
    if (logs.isEmpty()) {
        EmptyLogsView(
            icon = Icons.Default.PhoneCallback,
            text = "Nenhuma chamada bloqueada encontrada.\nSeu telefone está livre de perturbações!"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(logs) { log ->
                CallLogCard(log = log, onAddToWhitelist = onAddToWhitelist, onReport = onReport)
            }
        }
    }
}

@Composable
fun SmsLogsList(
    logs: List<SmsLog>,
    onAddToWhitelist: (String) -> Unit,
    onReport: (String) -> Unit
) {
    if (logs.isEmpty()) {
        EmptyLogsView(
            icon = Icons.Default.SmsFailed,
            text = "Nenhum SMS de spam bloqueado.\nCaixa de entrada limpa e segura!"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(logs) { log ->
                SmsLogCard(log = log, onAddToWhitelist = onAddToWhitelist, onReport = onReport)
            }
        }
    }
}

@Composable
fun CallLogCard(
    log: CallLog,
    onAddToWhitelist: (String) -> Unit,
    onReport: (String) -> Unit
) {
    val dateString = DateFormat.format("dd MMM yyyy, HH:mm", Date(log.timestamp)).toString()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (log.isBlocked) Icons.Default.Block else Icons.Default.CallReceived,
                        contentDescription = null,
                        tint = if (log.isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = log.number,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                ScoreBadge(score = log.spamScore)
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (!log.name.isNullOrBlank()) {
                Text(
                    text = "Identificado: ${log.name}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            if (log.isBlocked && !log.blockReason.isNullOrBlank()) {
                Text(
                    text = "Motivo: ${log.blockReason}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            Text(
                text = dateString,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onAddToWhitelist(log.number) },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Confiar (Whitelist)", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onReport(log.number) },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Denunciar", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SmsLogCard(
    log: SmsLog,
    onAddToWhitelist: (String) -> Unit,
    onReport: (String) -> Unit
) {
    val dateString = DateFormat.format("dd MMM yyyy, HH:mm", Date(log.timestamp)).toString()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (log.isBlocked) Icons.Default.SmsFailed else Icons.Default.Sms,
                        contentDescription = null,
                        tint = if (log.isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = log.number,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                ScoreBadge(score = log.spamScore)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = log.body,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (log.isBlocked && !log.blockReason.isNullOrBlank()) {
                Text(
                    text = "Bloqueado por: ${log.blockReason}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            Text(
                text = dateString,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { onAddToWhitelist(log.number) },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Confiar Remetente", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onReport(log.number) },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Denunciar", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ScoreBadge(score: Int) {
    val containerCol = when {
        score >= 70 -> MaterialTheme.colorScheme.errorContainer
        score >= 30 -> Color(0xFFFFF3CD) // Light warm amber
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val textCol = when {
        score >= 70 -> MaterialTheme.colorScheme.onErrorContainer
        score >= 30 -> Color(0xFF856404) // Dark amber text
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(containerCol)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$score% risco",
            color = textCol,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmptyLogsView(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}
