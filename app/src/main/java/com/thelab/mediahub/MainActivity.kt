package com.thelab.mediahub

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.thelab.mediahub.ui.screens.ApkInspectorScreen
import com.thelab.mediahub.ui.screens.MainDashboardScreen
import com.thelab.mediahub.ui.screens.SettingsScreen
import com.thelab.mediahub.ui.theme.AppThemeStyle
import com.thelab.mediahub.ui.theme.TheLabTheme
import com.thelab.mediahub.viewmodel.MediaViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MediaViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        autoGrantAllPermissions()

        setContent {
            var currentStyle by remember { mutableStateOf(AppThemeStyle.CRIMSON_NOIR) }

            TheLabTheme(themeStyle = currentStyle) {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("dashboard") {
                        MainDashboardScreen(
                            viewModel = viewModel,
                            onInspectApk = { apkPath ->
                                val encoded = java.net.URLEncoder.encode(apkPath, "UTF-8")
                                navController.navigate("inspector/$encoded")
                            },
                            onOpenSettings = { navController.navigate("settings") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            currentStyle = currentStyle,
                            onSelectStyle = { currentStyle = it },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("inspector/{apkPath}") { backStackEntry ->
                        val path = backStackEntry.arguments?.getString("apkPath") ?: ""
                        val decodedPath = java.net.URLDecoder.decode(path, "UTF-8")
                        ApkInspectorScreen(
                            apkPath = decodedPath,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    private fun autoGrantAllPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE
        )

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        permissionLauncher.launch(permissions.toTypedArray())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
