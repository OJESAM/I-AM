package com.example.kairoslivingstewards.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.kairoslivingstewards.data.local.entities.FellowshipEntity
import com.example.kairoslivingstewards.data.local.entities.FellowshipMemberEntity
import com.example.kairoslivingstewards.data.local.entities.FellowshipPostEntity
import com.example.kairoslivingstewards.data.local.entities.UserEntity
import com.example.kairoslivingstewards.ui.viewmodel.FellowshipViewModel

@Composable
fun FellowshipScreen(
    viewModel: FellowshipViewModel,
    currentUser: UserEntity
) {
    var selectedFellowship by remember { mutableStateOf<FellowshipEntity?>(null) }
    var showJoinDialog by remember { mutableStateOf(false) }

    if (selectedFellowship == null) {
        MainFellowshipList(
            viewModel = viewModel,
            onFellowshipClick = { 
                selectedFellowship = it
                viewModel.loadPosts(it.id)
                viewModel.loadMembers(it.id)
            },
            onJoinClick = { showJoinDialog = true }
        )
    } else {
        FellowshipDetailScreen(
            fellowship = selectedFellowship!!,
            viewModel = viewModel,
            currentUser = currentUser,
            onBack = { selectedFellowship = null }
        )
    }

    if (showJoinDialog) {
        JoinFellowshipDialog(
            onDismiss = { showJoinDialog = false },
            onJoin = { code ->
                viewModel.joinFellowship(currentUser.id, code)
                showJoinDialog = false
            }
        )
    }
}

@Composable
fun MemberManagementDialog(
    members: List<Pair<FellowshipMemberEntity, String>>,
    onDismiss: () -> Unit,
    onRemove: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Members") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(members) { (member, username) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(username, style = MaterialTheme.typography.bodyLarge)
                        if (member.role != "LEADER") {
                            IconButton(onClick = { onRemove(member.userId) }) {
                                Icon(Icons.Rounded.PersonRemove, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            Text("Leader", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFellowshipList(
    viewModel: FellowshipViewModel,
    onFellowshipClick: (FellowshipEntity) -> Unit,
    onJoinClick: () -> Unit
) {
    val fellowships by viewModel.allFellowships.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    val filteredFellowships = remember(fellowships, searchQuery) {
        fellowships.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            Column {
                LargeTopAppBar(
                    title = { Text("Fellowships", fontWeight = FontWeight.ExtraBold) },
                    actions = {
                        IconButton(onClick = onJoinClick) {
                            Icon(Icons.Rounded.Add, contentDescription = "Join Fellowship")
                        }
                    }
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search fellowships...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear")
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredFellowships) { fellowship ->
                FellowshipCard(fellowship, onClick = { onFellowshipClick(fellowship) })
            }
        }
    }
}

@Composable
fun FellowshipCard(fellowship: FellowshipEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Groups, contentDescription = null)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(fellowship.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(fellowship.description, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FellowshipDetailScreen(
    fellowship: FellowshipEntity,
    viewModel: FellowshipViewModel,
    currentUser: UserEntity,
    onBack: () -> Unit
) {
    val posts by viewModel.currentFellowshipPosts.collectAsStateWithLifecycle()
    val members by viewModel.currentFellowshipMembers.collectAsStateWithLifecycle()
    var postText by remember { mutableStateOf("") }
    var selectedMediaUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showMemberManagement by remember { mutableStateOf(false) }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedMediaUri = uri }
    )

    val isLeader = currentUser.id == fellowship.leaderId || currentUser.role == "LEADER"
    val isMember = members.any { it.first.userId == currentUser.id } || isLeader || currentUser.role == "ADMIN"
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(fellowship.name)
                        Text("Invite Code: ${fellowship.inviteCode}", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Join our fellowship '${fellowship.name}' on IAM! Use invite code: ${fellowship.inviteCode}")
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share Invite")
                    }
                    if (isLeader || currentUser.role == "ADMIN") {
                        IconButton(onClick = { showMemberManagement = true }) {
                            Icon(Icons.Rounded.ManageAccounts, contentDescription = "Manage Members")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (isMember || currentUser.role == "ADMIN") {
                Surface(tonalElevation = 3.dp) {
                    Column {
                        if (selectedMediaUri != null) {
                            Box(modifier = Modifier.padding(8.dp).size(80.dp)) {
                                AsyncImage(
                                    model = selectedMediaUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.small),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { selectedMediaUri = null },
                                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp).padding(4.dp)
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Remove media", tint = Color.White)
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.padding(8.dp).fillMaxWidth().imePadding(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                            }) {
                                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = "Add Media")
                            }
                            OutlinedTextField(
                                value = postText,
                                onValueChange = { postText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Post something...") },
                                shape = MaterialTheme.shapes.medium
                            )
                            IconButton(onClick = {
                                if (postText.isNotBlank() || selectedMediaUri != null) {
                                    viewModel.postToFellowship(
                                        fellowship.id,
                                        currentUser.id,
                                        currentUser.username,
                                        postText,
                                        selectedMediaUri?.toString()
                                    )
                                    postText = ""
                                    selectedMediaUri = null
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Post")
                            }
                        }
                    }
                }
            } else {
                Surface(tonalElevation = 3.dp) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp).imePadding(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Join this fellowship to participate in the chat",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(posts) { post ->
                PostItem(
                    post = post,
                    isDeletable = currentUser.role == "ADMIN" || currentUser.id == fellowship.leaderId || currentUser.id == post.userId,
                    onDelete = { viewModel.deletePost(post) }
                )
            }
        }
    }

    if (showMemberManagement) {
        MemberManagementDialog(
            members = members,
            onDismiss = { showMemberManagement = false },
            onRemove = { userId ->
                viewModel.removeMember(fellowship.id, userId)
            }
        )
    }
}

@Composable
fun PostItem(post: FellowshipPostEntity, isDeletable: Boolean, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape, modifier = Modifier.size(32.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(post.userName.take(1), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(post.userName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.weight(1f))
                if (isDeletable) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(post.content, style = MaterialTheme.typography.bodyLarge)
            if (post.mediaUrl != null) {
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = post.mediaUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun JoinFellowshipDialog(onDismiss: () -> Unit, onJoin: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join Fellowship") },
        text = {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Invite Code") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onJoin(code) }) { Text("Join") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
