package com.example.kairoslivingstewards.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kairoslivingstewards.data.local.entities.CommentEntity
import com.example.kairoslivingstewards.data.local.entities.UserEntity
import com.example.kairoslivingstewards.ui.viewmodel.LivestreamViewModel
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import android.content.Intent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivestreamScreen(viewModel: LivestreamViewModel, currentUser: UserEntity) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val videoId = settings?.youtubeVideoId ?: "dQw4w9WgXcQ"
    val commentsEnabled = settings?.commentsEnabled ?: true

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Church Livestream", fontWeight = FontWeight.ExtraBold) },
                actions = {
                    IconButton(onClick = {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Join our church livestream: https://www.youtube.com/watch?v=$videoId")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Livestream"))
                    }) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                YoutubePlayer(videoId = videoId)
                
                Column(modifier = Modifier.padding(24.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.LiveTv,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Sunday Worship Service",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    
                    Row(
                        modifier = Modifier.padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AssistChip(
                            onClick = { /* Like logic */ },
                            label = { Text("Like") },
                            leadingIcon = { Icon(Icons.Rounded.ThumbUp, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }

                    Text(
                        text = "Join us live as we worship together and hear a powerful word from our pastor. Experience the presence of God from wherever you are.",
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    PrimaryTabRow(selectedTabIndex = selectedTab) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                            Text("Live Chat", modifier = Modifier.padding(16.dp))
                        }
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                            Text("Personal Notes", modifier = Modifier.padding(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedTab == 0) {
                        if (commentsEnabled) {
                            LivestreamCommentInput { text ->
                                viewModel.addComment(currentUser.id, currentUser.username, text)
                            }
                        } else {
                            Text(
                                text = "Comments are disabled for this livestream",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        LivestreamNoteInput { text ->
                            viewModel.addNote(text)
                        }
                    }
                }
            }
            
            if (selectedTab == 0) {
                if (commentsEnabled) {
                    items(comments) { comment ->
                        LivestreamCommentItem(
                            comment = comment,
                            canDelete = currentUser.role == "ADMIN" || comment.userId == currentUser.id,
                            onDelete = { viewModel.deleteComment(comment) }
                        )
                    }
                }
            } else {
                items(notes) { note ->
                    LivestreamNoteItem(
                        note = note,
                        onDelete = { viewModel.deleteNote(note) }
                    )
                }
            }
        }
    }
}

@Composable
fun LivestreamNoteInput(onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Take a note...") },
            shape = MaterialTheme.shapes.large
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    onAdd(text)
                    text = ""
                }
            },
            enabled = text.isNotBlank()
        ) {
            Icon(Icons.Rounded.NoteAdd, contentDescription = "Add Note", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun LivestreamNoteItem(note: com.example.kairoslivingstewards.data.local.entities.NoteEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(note.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = note.content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun LivestreamCommentInput(onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Add a comment...") },
            shape = MaterialTheme.shapes.large
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    onSend(text)
                    text = ""
                }
            },
            enabled = text.isNotBlank()
        ) {
            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun LivestreamCommentItem(comment: CommentEntity, canDelete: Boolean = false, onDelete: () -> Unit = {}) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = comment.userName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            if (canDelete) {
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        }
        Text(text = comment.text, style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun YoutubePlayer(videoId: String) {
    val context = LocalContext.current
    AndroidView(
        factory = {
            val view = YouTubePlayerView(it)
            view.clipToOutline = true
            view.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) {
                    youTubePlayer.cueVideo(videoId, 0f)
                }
            })
            view
        },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16 / 9f)
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.extraLarge),
        update = { view ->
            // Update logic if videoId changes
        }
    )
}
