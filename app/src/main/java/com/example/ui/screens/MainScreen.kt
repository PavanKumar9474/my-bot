package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.database.ChatSession
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: ChatViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val sessions by viewModel.chatSessions.collectAsState()
    val selectedChatId by viewModel.selectedChatId.collectAsState()

    var showRenameDialog by remember { mutableStateOf<ChatSession?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    // Navigation dialogs
    if (showRenameDialog != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename Chat Session") },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    label = { Text("Session Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val session = showRenameDialog
                        if (session != null && renameInputText.trim().isNotEmpty()) {
                            viewModel.renameChat(session.id, renameInputText.trim())
                            showRenameDialog = null
                            Toast.makeText(context, "Session renamed!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(310.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    // Drawer Header with our beautiful generated logo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp, top = 12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_chatatm_logo),
                            contentDescription = "ChatATM Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ChatATM AI",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Multilingual Workspaces",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Divider
                    Divider(modifier = Modifier.padding(bottom = 12.dp))

                    // Start New Chat session button
                    Button(
                        onClick = {
                            viewModel.createNewChat("Session ${sessions.size + 1}")
                            scope.launch { drawerState.close() }
                            Toast.makeText(context, "New chat created!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New chat")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Chat Session", fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "PREVIOUS CONVERSATIONS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Scrollable Chat sessions list
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(sessions) { session ->
                            val isSelected = session.id == selectedChatId
                            
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                    } else {
                                        Color.Transparent
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable {
                                        viewModel.selectChat(session.id)
                                        scope.launch { drawerState.close() }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (session.isPinned) Icons.Default.PushPin else Icons.Default.ChatBubbleOutline,
                                            contentDescription = "Session Icon",
                                            tint = if (session.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = session.title,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }

                                    // Session action icons
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Pin icon toggle
                                        IconButton(
                                            onClick = { viewModel.togglePinChat(session.id, !session.isPinned) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (session.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                                                contentDescription = "Pin session",
                                                tint = if (session.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }

                                        // Rename icon toggle
                                        IconButton(
                                            onClick = {
                                                showRenameDialog = session
                                                renameInputText = session.title
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Rename session",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }

                                        // Delete icon toggle
                                        IconButton(
                                            onClick = { viewModel.deleteChat(session.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete session",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_chatatm_logo),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ChatATM",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Chat History sidebar"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    NavigationBarItem(
                        selected = viewModel.activeTab == "chat",
                        onClick = { viewModel.activeTab = "chat" },
                        icon = { Icon(Icons.Default.ChatBubble, contentDescription = "AI Chat") },
                        label = { Text("Chat", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = viewModel.activeTab == "translation",
                        onClick = { viewModel.activeTab = "translation" },
                        icon = { Icon(Icons.Default.Translate, contentDescription = "Translate") },
                        label = { Text("Translate", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = viewModel.activeTab == "ocr",
                        onClick = { viewModel.activeTab = "ocr" },
                        icon = { Icon(Icons.Default.DocumentScanner, contentDescription = "Scanner") },
                        label = { Text("OCR Scan", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = viewModel.activeTab == "writing",
                        onClick = { viewModel.activeTab = "writing" },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Templates") },
                        label = { Text("AI Tools", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = viewModel.activeTab == "settings",
                        onClick = { viewModel.activeTab = "settings" },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Crossfade animations between tabs for top tier UX
                Crossfade(targetState = viewModel.activeTab) { tab ->
                    when (tab) {
                        "chat" -> ChatScreen(viewModel = viewModel)
                        "translation" -> TranslationScreen(viewModel = viewModel)
                        "ocr" -> OcrScreen(viewModel = viewModel)
                        "writing" -> WritingToolsScreen(viewModel = viewModel)
                        "settings" -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
