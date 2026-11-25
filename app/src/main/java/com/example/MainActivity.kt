package com.example

import android.app.Application
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.firebase.GoogleAuthHelper
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

private data class BottomNavTab(
    val route: String,
    val baseRoute: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val mainBottomTabs = listOf(
    BottomNavTab("home", "home", "Beranda", Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_home"),
    BottomNavTab("quran", "quran", "Al-Qur'an", Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "nav_tab_quran"),
    BottomNavTab("prayers", "prayers", "Jadwal", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_tab_prayers"),
    BottomNavTab("qibla", "qibla", "Kiblat", Icons.Filled.Explore, Icons.Outlined.Explore, "nav_tab_qibla"),
    BottomNavTab("profile", "profile", "Profil", Icons.Filled.Person, Icons.Outlined.Person, "nav_tab_profile")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PayoShalatRoot()
            }
        }
    }
}

@Composable
private fun PayoShalatRoot() {
    val context = LocalContext.current
    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }
    var isCheckingSession by remember { mutableStateOf(Firebase.auth.currentUser == null) }

    // Observe Firebase Auth state reactively across sign-in, sign-out, and account switches
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            if (auth.currentUser != null) {
                isCheckingSession = false
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    // Attempt silent auto sign-in for returning Google users
    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            GoogleAuthHelper.attemptAutoSignIn(context)
        }
        isCheckingSession = false
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(isCheckingSession = isCheckingSession)
    } else {
        // Strict key(uid) scope ensures clean disposal of previous user's Firestore listeners on sign-out/switch
        key(user.uid) {
            AuthenticatedPayoShalatApp(uid = user.uid)
        }
    }
}

@Composable
private fun AuthenticatedPayoShalatApp(uid: String) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = viewModel(
        key = "payo_shalat_vm_$uid",
        factory = MainViewModelFactory(application, uid)
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val cloudSyncError by mainViewModel.cloudSyncError.collectAsState()

    LaunchedEffect(cloudSyncError) {
        val err = cloudSyncError
        if (!err.isNullOrBlank()) {
            snackbarHostState.showSnackbar(
                message = err,
                duration = SnackbarDuration.Short
            )
            mainViewModel.clearCloudSyncError()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRouteBase = navBackStackEntry?.destination?.route?.substringBefore("?") ?: "home"
    val showBottomBar = currentRouteBase in setOf("home", "quran", "prayers", "qibla", "profile")

    fun navigateToRootTab(targetRoute: String) {
        navController.navigate(targetRoute) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = WarmCream,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    color = PureWhite,
                    tonalElevation = 0.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(0.8.dp, SageBorder),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    NavigationBar(
                        containerColor = PureWhite,
                        tonalElevation = 0.dp,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        mainBottomTabs.forEach { tab ->
                            val isSelected = currentRouteBase == tab.baseRoute
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (!isSelected) {
                                        navigateToRootTab(tab.route)
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DeepSage,
                                    selectedTextColor = DeepSage,
                                    indicatorColor = SoftSageLight,
                                    unselectedIconColor = MutedOliveGray,
                                    unselectedTextColor = MutedOliveGray
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(animationSpec = tween(240)) },
            exitTransition = { fadeOut(animationSpec = tween(240)) },
            popEnterTransition = { fadeIn(animationSpec = tween(240)) },
            popExitTransition = { fadeOut(animationSpec = tween(240)) }
        ) {
            // 1. Beranda (Home)
            composable("home") {
                HomeScreen(
                    viewModel = mainViewModel,
                    onNavigateToQuran = { surahId ->
                        if (surahId != null) {
                            navController.navigate("quran?surahId=$surahId") {
                                launchSingleTop = true
                            }
                        } else {
                            navigateToRootTab("quran")
                        }
                    },
                    onNavigateToPrayers = { navigateToRootTab("prayers") },
                    onNavigateToQibla = { navigateToRootTab("qibla") },
                    onNavigateToTasbih = { navController.navigate("tasbih") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToProfile = { navigateToRootTab("profile") },
                    onNavigateToHadith = { category ->
                        if (!category.isNullOrBlank()) {
                            val encodedCategory = Uri.encode(category)
                            navController.navigate("hadith?category=$encodedCategory")
                        } else {
                            navController.navigate("hadith")
                        }
                    },
                    bottomContentPadding = innerPadding
                )
            }

            // 2. Al-Qur'an
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
                val surahId = backStackEntry.arguments?.getString("surahId")?.toIntOrNull()
                QuranScreen(
                    viewModel = mainViewModel,
                    onBack = { navigateToRootTab("home") },
                    initialSurahId = surahId
                )
            }

            // 3. Jadwal & Riwayat
            composable("prayers") {
                PrayerTimesScreen(
                    viewModel = mainViewModel,
                    onBack = { navigateToRootTab("home") },
                    showBackButton = false,
                    bottomContentPadding = innerPadding
                )
            }

            // 4. Kiblat
            composable("qibla") {
                QiblaScreen(
                    viewModel = mainViewModel,
                    onBack = { navigateToRootTab("home") }
                )
            }

            // 5. Profil
            composable("profile") {
                ProfileScreen(
                    viewModel = mainViewModel,
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToTasbih = { navController.navigate("tasbih") },
                    onNavigateToHadith = { navController.navigate("hadith") },
                    bottomContentPadding = innerPadding
                )
            }

            // Sub-screens
            composable("tasbih") {
                TasbihScreen(
                    viewModel = mainViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

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
                    Uri.decode(it)
                }
                HadithScreen(
                    viewModel = mainViewModel,
                    onBack = { navController.popBackStack() },
                    initialCategory = categoryArg
                )
            }

            composable("settings") {
                SettingsScreen(
                    viewModel = mainViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
