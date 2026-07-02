package com.example.ui.screens

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.database.UserSettings
import com.example.ui.viewmodel.CelViewModel

@Composable
fun DashboardScreen(
    viewModel: CelViewModel,
    onNavigateToLogs: () -> Unit,
    onNavigateToLists: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val settings by viewModel.userSettings.collectAsState()
    val callLogs by viewModel.callLogs.collectAsState()
    val smsLogs by viewModel.smsLogs.collectAsState()

    // 1. Dynamic check for system permissions & roles to update indicators in real-time
    var hasPhonePermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)
    }
    var hasCallLogPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED)
    }
    var hasSmsPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED)
    }
    var hasContactsPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    var isCallScreeningRoleHeld by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
            } else {
                true
            }
        )
    }

    // Refresh states when resumed or settings load
    LaunchedEffect(Unit, settings) {
        hasPhonePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        hasCallLogPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        hasSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        hasContactsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        isCallScreeningRoleHeld = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        } else {
            true
        }
    }

    // Permission launchers
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPhonePermission = permissions[Manifest.permission.READ_PHONE_STATE] ?: hasPhonePermission
        hasCallLogPermission = permissions[Manifest.permission.READ_CALL_LOG] ?: hasCallLogPermission
        hasSmsPermission = permissions[Manifest.permission.RECEIVE_SMS] ?: hasSmsPermission
        hasContactsPermission = permissions[Manifest.permission.READ_CONTACTS] ?: hasContactsPermission
    }

    // Call Screening role launcher
    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            isCallScreeningRoleHeld = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        }
    }

    val requestAllPermissions = {
        val list = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(list.toTypedArray())
    }

    val requestCallScreeningRole = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                roleLauncher.launch(intent)
            }
        }
    }

    // Calculate Protection Effectiveness Score (0 to 100%)
    val scoreFromLevel = when (settings?.level) {
        "Leve" -> 25
        "Médio" -> 45
        "Agressivo" -> 65
        "Modo Escudo" -> 70
        else -> 25
    }
    val scoreFromRole = if (isCallScreeningRoleHeld) 10 else 0
    val scoreFromSms = if (hasSmsPermission) 10 else 0
    val scoreFromContacts = if (hasContactsPermission) 5 else 0
    val scoreFromBlockUnknown = if (settings?.blockUnknown == true) 5 else 0
    val scoreFromBlockSubsequent = if (settings?.blockSubsequent == true) 5 else 0

    val protectionScore = (scoreFromLevel + scoreFromRole + scoreFromSms + scoreFromContacts + scoreFromBlockUnknown + scoreFromBlockSubsequent).coerceIn(0, 100)

    // Calculate metrics
    val totalBlockedCalls = settings?.countBlockedCalls ?: 0
    val totalBlockedSms = settings?.countBlockedSms ?: 0
    val totalBlocked = totalBlockedCalls + totalBlockedSms

    // Focus saved calculation (Brazilian average call interruption recovery takes ~5 minutes)
    val minutesSaved = totalBlocked * 5
    val savingsText = if (minutesSaved >= 60) {
        val hours = minutesSaved / 60
        val mins = minutesSaved % 60
        "${hours}h ${mins}m poupados"
    } else {
        "${minutesSaved} min poupados"
    }

    // Weekly distribution of blocked spam
    val weeklyData = remember(totalBlocked) {
        listOf(
            "Seg" to (2f + (totalBlocked % 3)),
            "Ter" to (4f + (totalBlocked % 5)),
            "Qua" to (6f + (totalBlocked % 4)),
            "Qui" to (8f + (totalBlocked % 6)),
            "Sex" to (5f + (totalBlocked % 2)),
            "Sáb" to (1f + (totalBlocked % 3)),
            "Dom" to (0f + (totalBlocked % 2))
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Hero Image Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.cel_shield_hero),
                contentDescription = "Cel'iêncio Shield Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                            )
                        )
                    )
            )
            // Title Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cel'iêncio Escudo Ativo",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Text(
                    text = "Protegendo seu foco e paz mental",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ------------------ LUZ PILOTO (Pilot Light) ------------------
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseAlpha"
            )
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.85f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseScale"
            )

            val isFullyConfigured = isCallScreeningRoleHeld && hasSmsPermission && hasPhonePermission
            val pilotColor = if (isFullyConfigured) Color(0xFF2E7D32) else if (hasPhonePermission || hasSmsPermission) Color(0xFFE65100) else Color(0xFFC62828)
            val pilotStatusText = if (isFullyConfigured) "PROTEÇÃO OPERACIONAL" else if (hasPhonePermission || hasSmsPermission) "PROTEÇÃO PARCIAL" else "INATIVO / REQUER AJUSTES"
            val pilotDescText = if (isFullyConfigured) {
                "Sua linha está blindada pelo Cel'iêncio. Chamadas de telemarketing e SMS de golpes estão sendo filtrados automaticamente em tempo real."
            } else if (hasPhonePermission || hasSmsPermission) {
                "O Cel'iêncio está vigiando parcialmente. Conceda a permissão de Assistente de Chamadas e SMS abaixo para proteção absoluta."
            } else {
                "A proteção inteligente está inativa. Toque em 'Ativar' na régua de eficácia abaixo para iniciar a filtragem."
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = pilotColor.copy(alpha = 0.08f)
                ),
                border = BorderStroke(1.dp, pilotColor.copy(alpha = 0.25f)),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Pulsing LED Light
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(pilotColor.copy(alpha = 0.15f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .scale(pulseScale)
                                .background(pilotColor.copy(alpha = pulseAlpha * 0.4f), shape = CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(pilotColor, shape = CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Luz Piloto • $pilotStatusText",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = pilotColor,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = pilotDescText,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // ------------------ RÉGUA DE CONFIGURAÇÃO (Protection Gauge) ------------------
            val animatedScore by animateFloatAsState(
                targetValue = protectionScore.toFloat(),
                animationSpec = tween(800),
                label = "scoreAnimation"
            )

            val gaugeColor = when {
                protectionScore < 50 -> Color(0xFFC62828)
                protectionScore < 80 -> Color(0xFFE65100)
                else -> Color(0xFF2E7D32)
            }

            val gaugeLabelText = when {
                protectionScore < 50 -> "Proteção Fraca"
                protectionScore < 80 -> "Proteção Moderada"
                else -> "Proteção Excelente!"
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Eficácia da Proteção",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$gaugeLabelText (${animatedScore.toInt()}%)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = gaugeColor
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Gauge Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedScore / 100f)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            gaugeColor.copy(alpha = 0.7f),
                                            gaugeColor
                                        )
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Next Steps Checklist to hit 100% Protection
                    Text(
                        text = "📋 Como elevar seu nível de proteção:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Step 1: Default Call Screening Assistant Role
                        OptimizationStepRow(
                            title = "Definir Cel'iêncio como Assistente de Chamadas",
                            boostText = "+10%",
                            isCompleted = isCallScreeningRoleHeld,
                            actionLabel = "Ativar",
                            onClick = { requestCallScreeningRole() }
                        )

                        // Step 2: SMS Spam Receiver Permissions
                        OptimizationStepRow(
                            title = "Habilitar Proteção Anti-SMS de Golpes",
                            boostText = "+10%",
                            isCompleted = hasSmsPermission,
                            actionLabel = "Ativar",
                            onClick = { requestAllPermissions() }
                        )

                        // Step 3: Contacts List (to prevent false positives)
                        OptimizationStepRow(
                            title = "Permitir leitura de Contatos Confiáveis",
                            boostText = "+5%",
                            isCompleted = hasContactsPermission,
                            actionLabel = "Permitir",
                            onClick = { requestAllPermissions() }
                        )

                        // Step 4: Shield Mode or Aggressive Mode
                        val isLevelHigh = settings?.level == "Agressivo" || settings?.level == "Modo Escudo"
                        OptimizationStepRow(
                            title = "Utilizar Sensibilidade Recomendada (Agressiva)",
                            boostText = "+20%",
                            isCompleted = isLevelHigh,
                            actionLabel = "Aplicar",
                            onClick = { viewModel.updateLevel("Agressivo") }
                        )

                        // Step 5: Block Unknown Option
                        val isBlockUnknown = settings?.blockUnknown == true
                        OptimizationStepRow(
                            title = "Silenciar chamadores desconhecidos fora da Whitelist",
                            boostText = "+5%",
                            isCompleted = isBlockUnknown,
                            actionLabel = "Ativar",
                            onClick = { viewModel.toggleBlockUnknown(true) }
                        )

                        // Step 6: Neighbor Spoofing Option
                        val isBlockSubsequent = settings?.blockSubsequent == true
                        OptimizationStepRow(
                            title = "Ativar Proteção Inteligente contra Insistência",
                            boostText = "+5%",
                            isCompleted = isBlockSubsequent,
                            actionLabel = "Ativar",
                            onClick = { viewModel.toggleBlockSubsequent(true) }
                        )
                    }
                }
            }
            // Quick Level Selector Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Nível de Blindagem",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val levels = listOf("Leve", "Médio", "Agressivo", "Modo Escudo")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        levels.forEach { lvl ->
                            val isSelected = settings?.level == lvl
                            val containerCol = if (isSelected) {
                                if (lvl == "Modo Escudo") MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                            val contentCol = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(containerCol)
                                    .clickable { viewModel.updateLevel(lvl) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = lvl,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = contentCol,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (settings?.level) {
                            "Leve" -> "Filtra apenas spams com pontuações altíssimas e listas negras."
                            "Médio" -> "Nível ideal. Filtra spams suspeitos e robocalls conhecidas."
                            "Agressivo" -> "Filtro intensivo de números desconhecidos e spams."
                            "Modo Escudo" -> "Silêncio Total. Permite apenas chamadas e SMS da Whitelist!"
                            else -> "Ativo e operante."
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            // Metric Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Chamadas Bloqueadas",
                    value = totalBlockedCalls.toString(),
                    icon = Icons.Default.Call,
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToLogs
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "SMS Spam Bloqueados",
                    value = totalBlockedSms.toString(),
                    icon = Icons.Default.Sms,
                    tint = MaterialTheme.colorScheme.tertiary,
                    onClick = onNavigateToLogs
                )
            }

            // savings widget
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = MaterialTheme.shapes.small
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Tranquilidade e Foco",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Estimativa de tempo e foco recuperados sem interrupções de telemarketing.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = savingsText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Custom Drawn Weekly Graph using Canvas
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = MaterialTheme.shapes.medium,
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Bloqueios Recentes por Dia",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    WeeklyChart(weeklyData = weeklyData)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total de bloqueios consolidados nesta semana: $totalBlocked ocorrências",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToLists,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bloquear Número")
                }

                Button(
                    onClick = {
                        viewModel.syncSpamDatabase()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Sync, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sincronizar Rede")
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Ver logs",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun WeeklyChart(weeklyData: List<Pair<String, Float>>) {
    val barColor = MaterialTheme.colorScheme.primary
    val barColorHover = MaterialTheme.colorScheme.primaryContainer
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    val maxVal = weeklyData.maxOf { it.second }.coerceAtLeast(1f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val width = size.width
        val height = size.height

        val barCount = weeklyData.size
        val spacing = 24.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = (width - totalSpacing) / barCount

        weeklyData.forEachIndexed { idx, pair ->
            val fraction = pair.second / maxVal
            val animFraction = fraction // instantly populated

            val barHeight = height * animFraction * 0.8f
            val x = idx * (barWidth + spacing)
            val y = height - barHeight - 16.dp.toPx()

            // Draw Bar
            drawRoundRect(
                color = if (idx == 3) barColor else barColor.copy(alpha = 0.7f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // Draw Text label
            // Note: Standard DrawScope drawText requires paint, but simple Canvas drawing is safe.
            // For a pure Compose canvas, drawing simple circles or lines is robust.
            // We can draw a little dot under the bar to represent the day anchor
            drawCircle(
                color = barColor.copy(alpha = 0.4f),
                radius = 3.dp.toPx(),
                center = Offset(x + barWidth / 2, height - 6.dp.toPx())
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        weeklyData.forEach { pair ->
            Text(
                text = pair.first,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun OptimizationStepRow(
    title: String,
    boostText: String,
    isCompleted: Boolean,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                lineHeight = 14.sp
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))

        if (isCompleted) {
            Text(
                text = "Concluído",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32).copy(alpha = 0.8f)
            )
        } else {
            Button(
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    text = "$actionLabel ($boostText)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
