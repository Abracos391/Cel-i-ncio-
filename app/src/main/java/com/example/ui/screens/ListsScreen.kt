package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.Blacklist
import com.example.data.database.Whitelist
import com.example.ui.viewmodel.CelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(viewModel: CelViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Lista Negra", "Lista Branca")

    val blacklist by viewModel.blacklist.collectAsState()
    val whitelist by viewModel.whitelist.collectAsState()

    var showAddBlacklistDialog by remember { mutableStateOf(false) }
    var showAddWhitelistDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listas Personalizadas", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) showAddBlacklistDialog = true
                    else showAddWhitelistDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Adicionar Número")
            }
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
                0 -> BlacklistTab(
                    list = blacklist,
                    onDelete = { viewModel.removeNumberFromBlacklist(it) }
                )
                1 -> WhitelistTab(
                    list = whitelist,
                    onDelete = { viewModel.removeNumberFromWhitelist(it) }
                )
            }
        }
    }

    // Add Blacklist Dialog
    if (showAddBlacklistDialog) {
        var number by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var reason by remember { mutableStateOf("Telemarketing Abusivo") }

        AlertDialog(
            onDismissRequest = { showAddBlacklistDialog = false },
            title = { Text("Bloquear Número", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = number,
                        onValueChange = { number = it },
                        label = { Text("Número de Telefone") },
                        placeholder = { Text("Ex: 11999998888") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Identificação / Empresa") },
                        placeholder = { Text("Ex: Cobrança Indesejada") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Business, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Motivo do Bloqueio") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Edit, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (number.isNotBlank()) {
                            viewModel.addNumberToBlacklist(number, name.ifBlank { "Lista Negra" }, reason)
                            showAddBlacklistDialog = false
                        }
                    }
                ) {
                    Text("Bloquear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBlacklistDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Add Whitelist Dialog
    if (showAddWhitelistDialog) {
        var number by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddWhitelistDialog = false },
            title = { Text("Confiar em Número", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = number,
                        onValueChange = { number = it },
                        label = { Text("Número de Telefone") },
                        placeholder = { Text("Ex: 11988887777") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome do Contato") },
                        placeholder = { Text("Ex: Minha Mãe, Dr. Silva") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (number.isNotBlank()) {
                            viewModel.addNumberToWhitelist(number, name.ifBlank { "Contato Confiável" })
                            showAddWhitelistDialog = false
                        }
                    }
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWhitelistDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun BlacklistTab(
    list: List<Blacklist>,
    onDelete: (String) -> Unit
) {
    if (list.isEmpty()) {
        EmptyLogsView(
            icon = Icons.Default.PlaylistAddCheck,
            text = "Sua Lista Negra está vazia.\nUse o botão de '+' abaixo para bloquear números manualmente!"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(list) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.number,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Nome: ${item.name}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Motivo: ${item.reason}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        IconButton(onClick = { onDelete(item.number) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir bloqueio",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WhitelistTab(
    list: List<Whitelist>,
    onDelete: (String) -> Unit
) {
    if (list.isEmpty()) {
        EmptyLogsView(
            icon = Icons.Default.PersonAdd,
            text = "Sua Lista Branca está vazia.\nAdicione números importantes de familiares ou empresas para garantir que eles nunca sejam bloqueados pelo robô!"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(list) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.number,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Nome: ${item.name}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        IconButton(onClick = { onDelete(item.number) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir da whitelist",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
