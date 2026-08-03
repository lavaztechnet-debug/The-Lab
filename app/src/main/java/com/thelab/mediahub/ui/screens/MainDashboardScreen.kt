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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thelab.mediahub.data.FileCategory
import com.thelab.mediahub.data.MediaEntity
import com.thelab.mediahub.ui.theme.*
import com.thelab.mediahub.viewmodel.MediaViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class TimeRangeFilter(val label: String, val days: Long?) {
    HOURS_24("24h", 1),
    DAYS_7("7d", 7),
    DAYS_30("30d", 30),
    ALL("All 2026", null)
}

@Composable
fun MainDashboardScreen(
    viewModel: MediaViewModel,
    onInspectApk: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val items by viewModel.mediaItems.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val is2026Only by viewModel.is2026Only.collectAsState()
    val selectedRange by viewModel.selectedRange.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedItemForDetail by remember { mutableStateOf<MediaEntity?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
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
                text = "THE-LAB 2026",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row {
                Button(
                    onClick = { viewModel.triggerManualSweep(context) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (isScanning) "Sweeping..." else "Full Sweep", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onOpenSettings) {
                    Text("⚙️", fontSize = 18.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        FilterChips2026Row(
            selectedRange = selectedRange,
            onSelectRange = { viewModel.setTimeRange(it) },
            is2026Only = is2026Only,
            onToggle2026Only = { viewModel.set2026Only(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                viewModel.search(it)
            },
            placeholder = { Text("Search 2026 media, prompts, docs...") },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .neuDepth(shadowRadius = 4.dp, offset = 2.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items) { item ->
                Media2026ItemCard(
                    item = item,
                    onClick = { selectedItemForDetail = item }
                )
            }
        }
    }

    selectedItemForDetail?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedItemForDetail = null },
            confirmButton = {
                Button(onClick = {
                    viewModel.downloadMediaItem(item)
                    Toast.makeText(context, "Enqueued in DownloadManager", Toast.LENGTH_SHORT).show()
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
                    SourceBadge(path = item.uriString)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Category: ${item.category.name}")
                    Text("MIME: ${item.mimeType}")
                    Text("URI: ${item.uriString}", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Details: ${item.formattedLabel}", fontSize = 12.sp)
                }
            }
        )
    }
}

@Composable
fun FilterChips2026Row(
    selectedRange: TimeRangeFilter,
    onSelectRange: (TimeRangeFilter) -> Unit,
    is2026Only: Boolean,
    onToggle2026Only: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TimeRangeFilter.values().forEach { range ->
                val isSelected = selectedRange == range
                Surface(
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .neuDepth(shadowRadius = 4.dp, offset = 2.dp)
                        .clickable { onSelectRange(range) }
                ) {
                    Text(
                        text = range.label,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("2026 Only", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Switch(
                checked = is2026Only,
                onCheckedChange = onToggle2026Only
            )
        }
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
fun Media2026ItemCard(item: MediaEntity, onClick: () -> Unit) {
    val sevenDaysAgoMs = System.currentTimeMillis() - (7L * 24 * 3600 * 1000)
    val isFresh = item.sourceFreshEpoch >= sevenDaysAgoMs
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

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
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                if (isFresh) {
                    Surface(
                        color = Color(0xFFEF4444),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Text(
                            text = "[2026 FRESH]",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Added: ${dateFormat.format(Date(item.dateAddedEpoch))} • Modified: ${dateFormat.format(Date(item.dateModifiedEpoch))}",
                fontSize = 10.sp,
                color = Color.Gray
            )

            if (item.uriString.startsWith("http") || item.uriString.startsWith("network://")) {
                Text(
                    text = "Source Last-Modified: ${dateFormat.format(Date(item.sourceFreshEpoch))}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
