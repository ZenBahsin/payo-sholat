package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import com.example.ui.MainViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val mainViewModel: MainViewModel = viewModel()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Navigation Host to Route between Screens
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.fillMaxSize(),
                        enterTransition = { fadeIn(animationSpec = tween(350)) },
                        exitTransition = { fadeOut(animationSpec = tween(350)) },
                        popEnterTransition = { fadeIn(animationSpec = tween(350)) },
                        popExitTransition = { fadeOut(animationSpec = tween(350)) }
                    ) {
                        // Home Dashboard Screen
                        composable("home") {
                            HomeScreen(
                                viewModel = mainViewModel,
                                onNavigateToQuran = { surahId ->
                                    if (surahId != null) {
                                        navController.navigate("quran?surahId=$surahId")
                                    } else {
                                        navController.navigate("quran")
                                    }
                                },
                                onNavigateToPrayers = {
                                    navController.navigate("prayers")
                                },
                                onNavigateToQibla = {
                                    navController.navigate("qibla")
                                },
                                onNavigateToTasbih = {
                                    navController.navigate("tasbih")
                                },
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                },
                                onNavigateToHadith = { category ->
                                    if (!category.isNullOrBlank()) {
                                        val encodedCategory = android.net.Uri.encode(category)
                                        navController.navigate("hadith?category=$encodedCategory")
                                    } else {
                                        navController.navigate("hadith")
                                    }
                                }
                            )
                        }

                        // Daily Hadith & Category Browser Screen
                        composable(
                            route = "hadith?category={category}",
                            arguments = listOf(
                                navArgument("category") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            val categoryArg = backStackEntry.arguments?.getString("category")?.let {
                                android.net.Uri.decode(it)
                            }
                            HadithScreen(
                                viewModel = mainViewModel,
                                onBack = { navController.popBackStack() },
                                initialCategory = categoryArg
                            )
                        }

                        // Al-Qur'an Screen
                        composable(
                            route = "quran?surahId={surahId}",
                            arguments = listOf(
                                navArgument("surahId") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            val surahIdString = backStackEntry.arguments?.getString("surahId")
                            val surahId = surahIdString?.toIntOrNull()
                            
                            QuranScreen(
                                viewModel = mainViewModel,
                                onBack = { navController.popBackStack() },
                                initialSurahId = surahId
                            )
                        }

                        // Jadwal Shalat Screen
                        composable("prayers") {
                            PrayerTimesScreen(
                                viewModel = mainViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // Arah Kiblat Screen
                        composable("qibla") {
                            QiblaScreen(
                                viewModel = mainViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // Digital Tasbih Screen
                        composable("tasbih") {
                            TasbihScreen(
                                viewModel = mainViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // Settings/Configuration Screen
                        composable("settings") {
                            SettingsScreen(
                                viewModel = mainViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
