package com.example.kairoslivingstewards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kairoslivingstewards.data.local.entities.UserEntity
import com.example.kairoslivingstewards.ui.viewmodel.DirectMessageViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DirectMessageScreen(
    viewModel: DirectMessageViewModel,
    currentUser: UserEntity
) {
    var selectedUser by remember { mutableStateOf<UserEntity?>(null) }
    var showAllUsers by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser.id) {
        viewModel.setOnlineStatus(currentUser.id, true)
    }

    if (selectedUser == null) {
        RecentChatsScreen(
            viewModel = viewModel,
            currentUser = currentUser,
            onUserClick = { 
                selectedUser = it
                viewModel.loadMessages(currentUser.id, it.id)
                viewModel.observeRecipientStatus(it.id)
            },
            onStartNewChat = { showAllUsers = true }
        )

        if (showAllUsers) {
            AllUsersDialog(
                viewModel = viewModel,
                currentUser = currentUser,
                onDismiss = { showAllUsers = false },
                onUserSelect = {
                    selectedUser = it
                    viewModel.loadMessages(currentUser.id, it.id)
                    showAllUsers = false
                }
            )
        }
    } else {
        ChatDetailScreen(
            viewModel = viewModel,
            currentUser = currentUser,
            otherUser = selectedUser!!,
            onBack = { selectedUser = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentChatsScreen(
    viewModel: DirectMessageViewModel,
    currentUser: UserEntity,
    onUserClick: (UserEntity) -> Unit,
    onStartNewChat: () -> Unit
) {
    val recentUsers by viewModel.recentUsers.collectAsStateWithLifecycle()

    LaunchedEffect(currentUser.id) {
        viewModel.loadRecentChats(currentUser.id)
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(title = { Text("Messages", fontWeight = FontWeight.ExtraBold) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onStartNewChat) {
                Icon(Icons.Rounded.Add, contentDescription = "New Chat")
            }
        }
    ) { paddingValues ->
        if (recentUsers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No recent chats. Start a new one!", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recentUsers) { user ->
                    ChatListItem(user = user, onClick = { onUserClick(user) })
                }
            }
        }
    }
}

@Composable
fun ChatListItem(user: UserEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(user.username, fontWeight = FontWeight.Bold)
                    if (user.isOnline) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.Green, CircleShape)
                        )
                    }
                }
            },
            supportingContent = { 
                if (user.isOnline) Text("Online", color = Color.Green) 
                else Text("Offline", color = Color.Gray)
            },
            leadingContent = {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(24.dp))
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllUsersDialog(
    viewModel: DirectMessageViewModel,
    currentUser: UserEntity,
    onDismiss: () -> Unit,
    onUserSelect: (UserEntity) -> Unit
) {
    val users by viewModel.allUsers.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Start New Chat") },
        text = {
            Box(modifier = Modifier.heightIn(max = 400.dp)) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(users.filter { it.id != currentUser.id }) { user ->
                        ListItem(
                            headlineContent = { Text(user.username) },
                            supportingContent = { Text(user.role) },
                            leadingContent = {
                                Icon(Icons.Rounded.Person, contentDescription = null)
                            },
                            modifier = Modifier.clickable { onUserSelect(user) }
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    viewModel: DirectMessageViewModel,
    currentUser: UserEntity,
    otherUser: UserEntity,
    onBack: () -> Unit
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val recipientStatus by viewModel.recipientStatus.collectAsStateWithLifecycle()
    var messageText by remember { mutableStateOf("") }

    LaunchedEffect(messageText) {
        if (messageText.isNotEmpty()) {
            viewModel.setTypingStatus(currentUser.id, otherUser.id)
        } else {
            viewModel.setTypingStatus(currentUser.id, null)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(otherUser.username)
                        recipientStatus?.let { status ->
                            if (status.typingTo == currentUser.id) {
                                Text("typing...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            } else if (status.isOnline) {
                                Text("Online", style = MaterialTheme.typography.labelSmall, color = Color.Green)
                            } else {
                                val lastSeen = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(status.lastSeen))
                                Text("Last seen at $lastSeen", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier.padding(8.dp).fillMaxWidth().imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        shape = MaterialTheme.shapes.medium
                    )
                    IconButton(onClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(currentUser.id, otherUser.id, messageText)
                            messageText = ""
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send")
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = false // Consider true for chat, but existing data might need sorting change
        ) {
            items(messages) { message ->
                val isCurrentUser = message.senderId == currentUser.id
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrentUser) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
                                Text(
                                    text = time,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                                if (isCurrentUser) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (message.isRead) Icons.Rounded.DoneAll else Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = if (message.isRead) Color.Blue else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
