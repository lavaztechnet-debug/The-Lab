package com.thelab.mediahub.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thelab.mediahub.ui.theme.AppThemeStyle

@Composable
fun SettingsScreen(
    currentStyle: AppThemeStyle,
    onSelectStyle: (AppThemeStyle) -> Unit,
    onBack: () -> Unit
) {
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
                text = "Extracted Theme Selector",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Button(onClick = onBack) {
                Text("Close")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Extracted Palette Style", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                AppThemeStyle.values().forEach { style ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = style.name.replace("_", " "),
                            color = if (currentStyle == style) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        RadioButton(
                            selected = (currentStyle == style),
                            onClick = { onSelectStyle(style) }
                        )
                    }
                }
            }
        }
    }
}
