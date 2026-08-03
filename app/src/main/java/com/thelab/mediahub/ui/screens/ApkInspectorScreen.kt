package com.thelab.mediahub.ui.screens

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thelab.mediahub.ui.theme.CharcoalBg
import com.thelab.mediahub.ui.theme.ElectricBlue
import com.thelab.mediahub.ui.theme.TextGray
import com.thelab.mediahub.ui.theme.TextWhite

@Composable
fun ApkInspectorScreen(apkPath: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager
    val packageInfo = remember(apkPath) {
        try {
            pm.getPackageArchiveInfo(apkPath, PackageManager.GET_PERMISSIONS)
        } catch (e: Exception) {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
        ) {
            Text("Back to Dashboard")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "APK Package Inspector",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (packageInfo != null) {
            Text("Package Name: ${packageInfo.packageName}", color = ElectricBlue)
            Text("Version Name: ${packageInfo.versionName}", color = TextWhite)
            Text("Version Code: ${packageInfo.versionCode}", color = TextWhite)

            Spacer(modifier = Modifier.height(16.dp))

            Text("Requested Permissions:", fontWeight = FontWeight.Bold, color = TextWhite)
            packageInfo.requestedPermissions?.forEach { perm ->
                Text("• $perm", color = TextGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            } ?: Text("No permissions requested.", color = TextGray)
        } else {
            Text("Failed to parse APK manifest. File may be encrypted or corrupted.", color = TextGray)
        }
    }
}
