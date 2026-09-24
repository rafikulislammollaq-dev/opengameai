package ch.duartesantos.opengym

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import ch.duartesantos.opengym.data.repository.GymRepository
import ch.duartesantos.opengym.ui.screens.*
import ch.duartesantos.opengym.ui.theme.*
import ch.duartesantos.opengym.ui.viewmodel.GymViewModel
import ch.duartesantos.opengym.ui.viewmodel.GymViewModelFactory

sealed class Screen(val route: String, val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Home : Screen("home", "Today", Icons.Filled.Today, Icons.Outlined.Today)
    object LivePose : Screen("live_pose", "AI Coach", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome)
    object Plan : Screen("plan", "Plan", Icons.Filled.DateRange, Icons.Outlined.DateRange)
    object Exercises : Screen("exercises", "Exercises", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter)
    object History : Screen("history", "History", Icons.Filled.History, Icons.Outlined.History)
    object Stats : Screen("stats", "Stats", Icons.Filled.ShowChart, Icons.Outlined.ShowChart)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    object ActiveWorkout : Screen("active_workout", "Workout", Icons.Filled.PlayArrow, Icons.Outlined.PlayArrow)
}

class MainActivity : ComponentActivity() {

    private val viewModel: GymViewModel by viewModels {
        val app = application as OpenGymApp
        val repository = GymRepository(app.database, applicationContext)
        GymViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val accentColor = remember(settings.accentColorHex) {
                try {
                    Color(android.graphics.Color.parseColor(settings.accentColorHex))
                } catch (e: Exception) {
                    AccentLime
                }
            }

            OpenGymTheme(accentColor = accentColor) {
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScaffold(viewModel: GymViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val durationSeconds by viewModel.activeWorkoutDurationSeconds.collectAsStateWithLifecycle()

    val navItems = listOf(
        Screen.Home,
        Screen.LivePose,
        Screen.Plan,
        Screen.Exercises,
        Screen.History,
        Screen.Stats,
        Screen.Settings
    )

    val isWorkoutScreen = currentRoute == Screen.ActiveWorkout.route

    Scaffold(
        bottomBar = {
            if (!isWorkoutScreen) {
                Column {
                    // Floating Mini-bar if workout is active and user is browsing other screens
                    if (activeSession != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                                .clickable { navController.navigate(Screen.ActiveWorkout.route) }
                                .testTag("active_workout_minibar"),
                            color = DarkSurfaceElevated,
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = activeSession?.name ?: "Active Workout",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary
                                        )
                                        val min = durationSeconds / 60
                                        val sec = durationSeconds % 60
                                        Text(
                                            text = "%02d:%02d".format(min, sec),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Button(
                                    onClick = { navController.navigate(Screen.ActiveWorkout.route) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Resume", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    // Bottom Navigation Bar
                    NavigationBar(
                        containerColor = DarkSurface,
                        contentColor = TextPrimary,
                        tonalElevation = 8.dp
                    ) {
                        navItems.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_item_${screen.route}")
                            )
                        }
                    }
                }
            }
        },
        containerColor = BlackBg
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isWorkoutScreen) PaddingValues(0.dp) else paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToActiveWorkout = { navController.navigate(Screen.ActiveWorkout.route) },
                    onNavigateToPlan = { navController.navigate(Screen.Plan.route) },
                    onNavigateToStats = { navController.navigate(Screen.Stats.route) },
                    onNavigateToLivePose = { navController.navigate(Screen.LivePose.route) }
                )
            }
            composable(Screen.LivePose.route) {
                LivePoseScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Plan.route) {
                PlanScreen(
                    viewModel = viewModel,
                    onStartRoutine = { routine ->
                        viewModel.startWorkoutFromRoutine(routine)
                        navController.navigate(Screen.ActiveWorkout.route)
                    }
                )
            }
            composable(Screen.Exercises.route) {
                ExercisesScreen(viewModel = viewModel)
            }
            composable(Screen.History.route) {
                HistoryScreen(viewModel = viewModel)
            }
            composable(Screen.Stats.route) {
                StatsScreen(viewModel = viewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }
            composable(Screen.ActiveWorkout.route) {
                ActiveWorkoutScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
