package com.example.kairoslivingstewards.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kairoslivingstewards.data.local.entities.UserEntity
import com.example.kairoslivingstewards.ui.viewmodel.DirectMessageViewModel

@Composable
fun DirectMessageScreen(
    viewModel: DirectMessageViewModel,
    currentUser: UserEntity
) {
    var selectedUser by remember { mutableStateOf<UserEntity?>(null) }
    var showAllUsers by remember { mutableStateOf(false) }

    if (selectedUser == null) {
        RecentChatsScreen(
            viewModel = viewModel,
            currentUser = currentUser,
            onUserClick = { 
                selectedUser = it
                viewModel.loadMessages(currentUser.id, it.id)
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
            headlineContent = { Text(user.username, fontWeight = FontWeight.Bold) },
            supportingContent = { Text("Tap to view messages") },
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
    var messageText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(otherUser.username) },
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                val isCurrentUser = message.senderId == currentUser.id
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
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
                        Text(
                            text = message.content,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}
