package com.example.kairoslivingstewards.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kairoslivingstewards.data.local.entities.DirectMessageEntity
import com.example.kairoslivingstewards.data.local.entities.UserEntity
import com.example.kairoslivingstewards.ui.viewmodel.DirectMessageViewModel

@Composable
fun DirectMessageScreen(
    viewModel: DirectMessageViewModel,
    currentUser: UserEntity
) {
    var selectedUser by remember { mutableStateOf<UserEntity?>(null) }

    if (selectedUser == null) {
        UserListScreen(
            viewModel = viewModel,
            currentUser = currentUser,
            onUserClick = { 
                selectedUser = it
                viewModel.loadMessages(currentUser.id, it.id)
            }
        )
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
fun UserListScreen(
    viewModel: DirectMessageViewModel,
    currentUser: UserEntity,
    onUserClick: (UserEntity) -> Unit
) {
    val users by viewModel.allUsers.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(title = { Text("Direct Messages", fontWeight = FontWeight.ExtraBold) })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(users.filter { it.id != currentUser.id }) { user ->
                Card(
                    onClick = { onUserClick(user) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = { Text(user.username, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text(user.contact) },
                        leadingContent = {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Person, contentDescription = null)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
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
