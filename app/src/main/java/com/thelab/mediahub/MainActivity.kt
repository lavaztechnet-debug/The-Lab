package com.thelab.mediahub

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestStoragePermissions()

        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }
            var currentStyle by remember { mutableStateOf(AppThemeStyle.CHARCOAL) }

            TheLabTheme(darkTheme = isDarkTheme, themeStyle = currentStyle) {
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
                            isDarkTheme = isDarkTheme,
                            onToggleDarkTheme = { isDarkTheme = it },
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

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 101
            )
        }
    }
}
