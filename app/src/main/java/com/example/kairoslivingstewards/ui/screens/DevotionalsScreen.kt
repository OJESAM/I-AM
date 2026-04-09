package com.example.kairoslivingstewards.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.kairoslivingstewards.data.local.entities.CommentEntity
import com.example.kairoslivingstewards.data.local.entities.DevotionalEntity
import com.example.kairoslivingstewards.data.local.entities.UserEntity
import com.example.kairoslivingstewards.ui.viewmodel.DevotionalViewModel

@Composable
fun DevotionalsScreen(
    viewModel: DevotionalViewModel,
    currentUser: UserEntity
) {
    val devotionals by viewModel.devotionals.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    var selectedDevotional by remember { mutableStateOf<DevotionalEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    if (selectedDevotional == null) {
        DevotionalList(
            devotionals = devotionals,
            selectedCategory = selectedCategory,
            onCategorySelect = { viewModel.setCategory(it) },
            onDevotionalClick = { selectedDevotional = it },
            isAdmin = currentUser.role == "ADMIN",
            onAddClick = { showAddDialog = true }
        )
    } else {
        // Comments are still locally managed for now as per current DB structure
        val comments by viewModel.getCommentsForDevotional(selectedDevotional!!.id).collectAsStateWithLifecycle(initialValue = emptyList())
        
        DevotionalDetail(
            devotional = selectedDevotional!!,
            onBack = { selectedDevotional = null },
            onLike = { /* viewModel.updateLikes(it.id, it.likesCount + 1) */ },
            comments = comments,
            onAddComment = { text -> viewModel.addComment(selectedDevotional!!.id, currentUser.username, text) }
        )
    }

    if (showAddDialog) {
        AddDevotionalDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { newDevotional ->
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevotionalList(
    devotionals: List<DevotionalEntity>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    onDevotionalClick: (DevotionalEntity) -> Unit,
    isAdmin: Boolean,
    onAddClick: () -> Unit
) {
    val categories = listOf("All", "Faith", "Prayer", "Fasting", "Wisdom", "Grace")

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Daily Devotionals", fontWeight = FontWeight.ExtraBold) },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = onAddClick) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add Devotional")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { onCategorySelect(category) },
                        label = { Text(category) }
                    )
                }
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(devotionals) { devotional ->
                    DevotionalItem(devotional, onDevotionalClick)
                }
            }
        }
    }
}

@Composable
fun DevotionalItem(
    devotional: DevotionalEntity,
    onClick: (DevotionalEntity) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(devotional) },
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            devotional.imageUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = devotional.category,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = devotional.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = devotional.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = devotional.content,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevotionalDetail(
    devotional: DevotionalEntity,
    onBack: () -> Unit,
    onLike: (DevotionalEntity) -> Unit,
    comments: List<CommentEntity> = emptyList(),
    onAddComment: (String) -> Unit = {}
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Devotional", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    FilledTonalIconButton(
                        onClick = {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, devotional.title)
                                putExtra(Intent.EXTRA_TEXT, "${devotional.title}\n\n${devotional.scripture}\n\n${devotional.content}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Devotional"))
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
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
                devotional.imageUrl?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = devotional.category,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = devotional.date,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = devotional.title,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = MaterialTheme.typography.headlineLarge.lineHeight * 1.1
                    )
                    
                    Row(
                        modifier = Modifier.padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = { onLike(devotional) },
                            label = { Text("${devotional.likesCount} Likes") },
                            leadingIcon = { Icon(Icons.Rounded.ThumbUp, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.large
                    ) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = devotional.scripture,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = devotional.content,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    if (devotional.commentsEnabled) {
                        Text(
                            text = "Comments",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        DevotionalCommentInput { text: String ->
                            onAddComment(text)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
            
            if (devotional.commentsEnabled) {
                items(comments) { comment ->
                    DevotionalCommentItem(comment)
                }
            }
        }
    }
}

@Composable
fun DevotionalCommentInput(onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp),
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
fun DevotionalCommentItem(comment: CommentEntity) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(text = comment.userName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(text = comment.text, style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

