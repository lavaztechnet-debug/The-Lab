package com.thelab.mediahub.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thelab.mediahub.data.FileCategory
import com.thelab.mediahub.data.MediaItem
import com.thelab.mediahub.ui.theme.*
import com.thelab.mediahub.viewmodel.MediaViewModel

@Composable
fun MainDashboardScreen(
    viewModel: MediaViewModel,
    onInspectApk: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val items by viewModel.mediaItems.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val excludeLocal by viewModel.excludeLocal.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedItemForDetail by remember { mutableStateOf<MediaItem?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "THE-LAB",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row {
                Button(
                    onClick = { viewModel.triggerFullSweep() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (isScanning) "Scanning..." else "Full Sweep", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onOpenSettings) {
                    Text("⚙️", fontSize = 20.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                viewModel.search(it)
            },
            placeholder = { Text("Search packages, media, docs...") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips & Exclude Local Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(label = "All", onClick = { viewModel.filterByCategory(null) })
            FilterChip(label = "APKs", onClick = { viewModel.filterByCategory(FileCategory.PACKAGE) })
            FilterChip(label = "Photos", onClick = { viewModel.filterByCategory(FileCategory.PHOTO) })

            // Exclude Local Storage Filter Switch
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hide Local", fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground)
                Switch(
                    checked = excludeLocal,
                    onCheckedChange = { viewModel.toggleExcludeLocal(it) },
                    modifier = Modifier.scale(0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items) { item ->
                MediaItemCard(
                    item = item,
                    onClick = { selectedItemForDetail = item },
                    onInspectApk = onInspectApk
                )
            }
        }
    }

    // Detail Dialog Window
    selectedItemForDetail?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedItemForDetail = null },
            confirmButton = {
                Button(onClick = {
                    viewModel.downloadMediaItem(item)
                    Toast.makeText(context, "Saved to /Download/The-Lab/", Toast.LENGTH_SHORT).show()
                    selectedItemForDetail = null
                }) {
                    Text("Download / Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedItemForDetail = null }) {
                    Text("Close")
                }
            },
            title = { Text(item.fileName, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    SourceBadge(path = item.path)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Category: ${item.category.name}")
                    Text("MIME: ${item.mimeType}")
                    Text("Path: ${item.path}", fontSize = 11.sp, color = TextGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Raw Details:", fontWeight = FontWeight.Bold)
                    Text(item.formattedLabel, fontSize = 12.sp)
                }
            }
        )
    }
}

@Composable
fun FilterChip(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
    }
}

@Composable
fun SourceBadge(path: String) {
    val (label, color) = when {
        path.startsWith("network://") -> "LAN DEVICE" to TagNetwork
        path.startsWith("web://movie/") -> "2026 MOVIE" to TagMovie
        path.startsWith("web://prompt/") -> "AI PROMPT" to TagWeb
        else -> "LOCAL FILE" to TagLocal
    }

    Surface(
        color = color,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun MediaItemCard(item: MediaItem, onClick: () -> Unit, onInspectApk: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.fileName,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                SourceBadge(path = item.path)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.formattedLabel,
                color = TextGray,
                fontSize = 12.sp
            )

            if (item.category == FileCategory.PACKAGE && item.fileName.endsWith(".apk")) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onInspectApk(item.path) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Inspect Manifest", fontSize = 11.sp)
                }
            }
        }
    }
}
