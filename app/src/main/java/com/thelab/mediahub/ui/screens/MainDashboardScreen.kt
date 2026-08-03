package com.thelab.mediahub.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thelab.mediahub.data.FileCategory
import com.thelab.mediahub.data.MediaItem
import com.thelab.mediahub.ui.theme.CharcoalBg
import com.thelab.mediahub.ui.theme.CharcoalCard
import com.thelab.mediahub.ui.theme.ElectricBlue
import com.thelab.mediahub.ui.theme.TextGray
import com.thelab.mediahub.ui.theme.TextWhite
import com.thelab.mediahub.viewmodel.MediaViewModel

@Composable
fun MainDashboardScreen(viewModel: MediaViewModel, onInspectApk: (String) -> Unit) {
    val items by viewModel.mediaItems.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBg)
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
                color = ElectricBlue
            )
            Button(
                onClick = { viewModel.triggerDirectorySweep() },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Text(if (isScanning) "Scanning..." else "Manual Sweep")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                viewModel.search(it)
            },
            placeholder = { Text("Search packages, media, docs...", color = TextGray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElectricBlue,
                unfocusedBorderColor = TextGray,
                focusedTextColor = TextWhite
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilterChip(label = "All", onClick = { viewModel.filterByCategory(null) })
            FilterChip(label = "APKs", onClick = { viewModel.filterByCategory(FileCategory.PACKAGE) })
            FilterChip(label = "Photos", onClick = { viewModel.filterByCategory(FileCategory.PHOTO) })
            FilterChip(label = "Docs", onClick = { viewModel.filterByCategory(FileCategory.DOCUMENT) })
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items) { item ->
                MediaItemCard(item = item, onInspectApk = onInspectApk)
            }
        }
    }
}

@Composable
fun FilterChip(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = CharcoalCard),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = TextWhite, fontSize = 12.sp)
    }
}

@Composable
fun MediaItemCard(item: MediaItem, onInspectApk: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalCard),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = item.fileName,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                fontSize = 14.sp
            )
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
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Inspect Manifest", fontSize = 11.sp)
                }
            }
        }
    }
}
