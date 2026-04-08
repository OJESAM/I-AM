package com.example.kairoslivingstewards.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kairoslivingstewards.data.local.entities.DevotionalEntity
import java.util.UUID

@Composable
fun AddDevotionalDialog(
    onDismiss: () -> Unit,
    onAdd: (DevotionalEntity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var scripture by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var imageUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Devotional") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                OutlinedTextField(value = scripture, onValueChange = { scripture = it }, label = { Text("Scripture") })
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    modifier = Modifier.height(150.dp)
                )
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") })
                OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Image URL (Optional)") })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAdd(
                        DevotionalEntity(
                            id = UUID.randomUUID().toString(),
                            title = title,
                            content = content,
                            scripture = scripture,
                            category = category,
                            imageUrl = if (imageUrl.isBlank()) null else imageUrl,
                            date = "Today",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                },
                enabled = title.isNotBlank() && content.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AdminAddFellowshipDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var leaderId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Fellowship") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Fellowship Name") })
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.height(100.dp)
                )
                OutlinedTextField(value = leaderId, onValueChange = { leaderId = it }, label = { Text("Leader User ID") })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(name, description, leaderId) },
                enabled = name.isNotBlank() && leaderId.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
