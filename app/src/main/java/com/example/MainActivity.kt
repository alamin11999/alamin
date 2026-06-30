package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.JarvisViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: JarvisViewModel = viewModel()
                val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()

                // Request recording permission at startup to enable voice decoding
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    // Permissions granted status logged or handled gracefully
                }

                LaunchedEffect(Unit) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (isFirstLaunch) {
                        InitializationScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        val navController = rememberNavController()

                        LaunchedEffect(Unit) {
                            viewModel.uiEvent.collect { event ->
                                when (event) {
                                    is JarvisViewModel.UiEvent.Navigate -> {
                                        if (event.route == "BACK") {
                                            navController.popBackStack()
                                        } else {
                                            navController.navigate(event.route)
                                        }
                                    }
                                    is JarvisViewModel.UiEvent.NavigateBack -> {
                                        navController.popBackStack()
                                    }
                                }
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("home") {
                                HomeScreen(
                                    viewModel = viewModel,
                                    navController = navController
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    navController = navController
                                )
                            }
                            composable("ai_settings") {
                                AiModelSettingsScreen(
                                    viewModel = viewModel,
                                    navController = navController
                                )
                            }
                            composable("app_settings") {
                                AppManagerScreen(
                                    viewModel = viewModel,
                                    navController = navController
                                )
                            }
                            composable("memory_settings") {
                                MemoryLogsScreen(
                                    viewModel = viewModel,
                                    navController = navController
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
