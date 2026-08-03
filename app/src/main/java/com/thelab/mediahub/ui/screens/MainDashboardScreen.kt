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
import androidx.compose.ui.draw.clip
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
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedItemForDetail by remember { mutableStateOf<MediaItem?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top Neumorphic 3D Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .neuDepth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "THE-LAB 3D",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row {
                Button(
                    onClick = { viewModel.triggerFullSweep() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (isScanning) "Scanning..." else "Full Sweep", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onOpenSettings) {
                    Text("⚙️", fontSize = 18.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Neumorphic Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                viewModel.search(it)
            },
            placeholder = { Text("Search packages, media, docs...") },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .neuDepth(shadowRadius = 4.dp, offset = 2.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Category Filter Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CategoryPill("All", selectedCategory == null || selectedCategory == FileCategory.ALL) {
                    viewModel.filterByCategory(FileCategory.ALL)
                }
                CategoryPill("Videos", selectedCategory == FileCategory.VIDEO) {
                    viewModel.filterByCategory(FileCategory.VIDEO)
                }
                CategoryPill("Docs", selectedCategory == FileCategory.DOCUMENT) {
                    viewModel.filterByCategory(FileCategory.DOCUMENT)
                }
                CategoryPill("APKs", selectedCategory == FileCategory.PACKAGE) {
                    viewModel.filterByCategory(FileCategory.PACKAGE)
                }
                CategoryPill("Photos", selectedCategory == FileCategory.PHOTO) {
                    viewModel.filterByCategory(FileCategory.PHOTO)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hide Local", fontSize = 9.sp)
                Switch(
                    checked = excludeLocal,
                    onCheckedChange = { viewModel.toggleExcludeLocal(it) },
                    modifier = Modifier.scale(0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Discovered Item List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items) { item ->
                NeumorphicItemCard(
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
                    Toast.makeText(context, "Download enqueued in system notification bar!", Toast.LENGTH_SHORT).show()
                    selectedItemForDetail = null
                }) {
                    Text("Download")
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
                    Text("Source: ${item.path}", fontSize = 11.sp, color = TextGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Details: ${item.formattedLabel}", fontSize = 12.sp)
                }
            }
        )
    }
}

@Composable
fun CategoryPill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .neuDepth(shadowRadius = 4.dp, offset = 2.dp)
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun SourceBadge(path: String) {
    val (label, color) = when {
        path.startsWith("network://") -> "LAN" to TagNetwork
        path.contains("archive.org") -> "HORROR MOVIE" to TagMovie
        path.contains("github") -> "PROMPT" to TagWeb
        else -> "LOCAL" to TagLocal
    }

    Surface(
        color = color,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun NeumorphicItemCard(item: MediaItem, onClick: () -> Unit, onInspectApk: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neuDepth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.fileName,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                SourceBadge(path = item.path)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.formattedLabel,
                color = TextGray,
                fontSize = 11.sp
            )

            if (item.category == FileCategory.PACKAGE && item.fileName.endsWith(".apk")) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onInspectApk(item.path) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Inspect Manifest", fontSize = 10.sp)
                }
            }
        }
    }
}
