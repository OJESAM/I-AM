package com.example.kairoslivingstewards.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kairoslivingstewards.data.local.entities.DevotionalEntity
import com.example.kairoslivingstewards.data.local.entities.UserEntity
import com.example.kairoslivingstewards.ui.viewmodel.DevotionalViewModel
import com.example.kairoslivingstewards.ui.viewmodel.FellowshipViewModel
import com.example.kairoslivingstewards.ui.viewmodel.LivestreamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    devotionalViewModel: DevotionalViewModel,
    fellowshipViewModel: FellowshipViewModel,
    livestreamViewModel: LivestreamViewModel,
    currentUser: UserEntity
) {
    var currentTab by remember { mutableIntStateOf(0) }
    
    val tabs = remember(currentUser.role) {
        mutableListOf("Content", "Fellowships", "Livestream").apply {
            if (currentUser.role == "ADMIN") {
                add("Users")
                add("Moderation")
            } else if (currentUser.role == "LEADER") {
                add("Moderation")
            }
        }.toList()
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("I AM Management", fontWeight = FontWeight.ExtraBold) }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            ScrollableTabRow(selectedTabIndex = currentTab, edgePadding = 16.dp) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = currentTab == index,
                        onClick = { currentTab = index },
                        text = { Text(title) }
                    )
                }
            }

            val selectedTabName = tabs.getOrNull(currentTab)
            when (selectedTabName) {
                "Content" -> DevotionalManagement(devotionalViewModel)
                "Fellowships" -> FellowshipManagement(fellowshipViewModel)
                "Livestream" -> LivestreamManagement(livestreamViewModel)
                "Users" -> UserManagement(fellowshipViewModel)
                "Moderation" -> ModerationPanel(fellowshipViewModel)
            }
        }
    }
}

@Composable
fun UserManagement(viewModel: FellowshipViewModel) {
    val users by viewModel.allUsers.collectAsStateWithLifecycle()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text(
                "User Control",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        items(users) { user ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                ListItem(
                    headlineContent = { Text(user.username) },
                    supportingContent = { Text("${user.contact} • Role: ${user.role}") },
                    trailingContent = {
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "Options")
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Make ADMIN") },
                                    onClick = { 
                                        viewModel.updateUserRole(user.id, "ADMIN")
                                        showMenu = false 
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Make LEADER") },
                                    onClick = { 
                                        viewModel.updateUserRole(user.id, "LEADER")
                                        showMenu = false 
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Make USER") },
                                    onClick = { 
                                        viewModel.updateUserRole(user.id, "USER")
                                        showMenu = false 
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Delete User", color = MaterialTheme.colorScheme.error) },
                                    onClick = { 
                                        // Implementation for deleting user
                                        showMenu = false 
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DevotionalManagement(viewModel: DevotionalViewModel) {
    val devotionals by viewModel.devotionals.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text("Add New Devotional")
            }
        }
        items(devotionals) { devotional ->
            ListItem(
                headlineContent = { Text(devotional.title) },
                supportingContent = { Text(devotional.category) },
                trailingContent = {
                    IconButton(onClick = { viewModel.deleteDevotional(devotional) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    }
    
    if (showAddDialog) {
        AddDevotionalDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { newDevotional: DevotionalEntity ->
                viewModel.addDevotional(
                    title = newDevotional.title,
                    content = newDevotional.content,
                    scripture = newDevotional.scripture,
                    category = newDevotional.category,
                    imageUrl = newDevotional.imageUrl
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
fun FellowshipManagement(viewModel: FellowshipViewModel) {
    val fellowships by viewModel.allFellowships.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Icon(Icons.Rounded.Groups, contentDescription = null)
                Text("Create New Fellowship")
            }
        }
        items(fellowships) { fellowship ->
            ListItem(
                headlineContent = { Text(fellowship.name) },
                supportingContent = { Text("Code: ${fellowship.inviteCode}") },
                trailingContent = {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                }
            )
        }
    }

    if (showAddDialog) {
        AdminAddFellowshipDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name: String, desc: String, leaderId: String -> 
                viewModel.createFellowship(name, desc, leaderId)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun LivestreamManagement(viewModel: LivestreamViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var videoId by remember(settings) { mutableStateOf(settings?.youtubeVideoId ?: "") }
    var commentsEnabled by remember(settings) { mutableStateOf(settings?.commentsEnabled ?: true) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = videoId,
            onValueChange = { videoId = it },
            label = { Text("YouTube Live Video ID") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Enable Live Chat", modifier = Modifier.weight(1f))
            Switch(checked = commentsEnabled, onCheckedChange = { commentsEnabled = it })
        }
        Button(
            onClick = { viewModel.updateSettings(videoId, commentsEnabled) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Save, contentDescription = null)
            Text("Save & Update Stream")
        }
    }
}

@Composable
fun ModerationPanel(viewModel: FellowshipViewModel) {
    val posts by viewModel.allPosts.collectAsStateWithLifecycle()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text(
                "Global Post Moderation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        items(posts) { post ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                ListItem(
                    headlineContent = { Text(post.content) },
                    supportingContent = { Text("By ${post.userName} in ${post.fellowshipId}") },
                    trailingContent = {
                        IconButton(onClick = { viewModel.deletePost(post) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }
        }
    }
}
