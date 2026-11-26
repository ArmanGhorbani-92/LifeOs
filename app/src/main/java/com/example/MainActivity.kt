package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.GlassCard
import com.example.ui.components.LifeOsBackground
import com.example.ui.navigation.LifeOsDestination
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.HabitsScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.PlannerScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.theme.LifeOsTheme
import com.example.ui.viewmodel.LifeOsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: LifeOsViewModel = viewModel()
            val preferences by viewModel.preferences.collectAsStateWithLifecycle()

            LifeOsTheme(themeMode = preferences.themeMode) {
                LifeOsAppRoot(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LifeOsAppRoot(viewModel: LifeOsViewModel) {
    val colors = LifeOsTheme.colors
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentDestination = LifeOsDestination.fromRoute(currentRoute)

    val feedback by viewModel.feedback.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMoreSheet by remember { mutableStateOf(false) }

    LaunchedEffect(feedback?.id) {
        feedback?.let { msg ->
            snackbarHostState.showSnackbar(msg.text)
            viewModel.clearFeedback()
        }
    }

    fun navigateTo(dest: LifeOsDestination) {
        navController.navigate(dest.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LifeOsBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 680.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = if (useNavigationRail) 16.dp else 84.dp)
                    ) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = colors.navyStrong,
                            contentColor = if (colors.isDark) colors.backgroundBase else Color.White,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            ) { _ ->
                if (useNavigationRail) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        NavigationRail(
                            containerColor = colors.glassSurface,
                            modifier = Modifier
                                .fillMaxHeight()
                                .border(width = 1.dp, color = colors.glassBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "LIFEOS",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.oliveAccent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                LifeOsDestination.entries.forEach { dest ->
                                    val selected = currentDestination == dest
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { navigateTo(dest) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = dest.titleFa
                                            )
                                        },
                                        label = { Text(dest.titleFa, maxLines = 1) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = Color.White,
                                            selectedTextColor = colors.oliveAccent,
                                            indicatorColor = colors.oliveAccent,
                                            unselectedIconColor = colors.textSecondary,
                                            unselectedTextColor = colors.textSecondary
                                        ),
                                        modifier = Modifier.testTag("rail_${dest.route}")
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            LifeOsNavHost(
                                navController = navController,
                                viewModel = viewModel,
                                onNavigate = ::navigateTo,
                                showSecondaryBack = false
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        LifeOsNavHost(
                            navController = navController,
                            viewModel = viewModel,
                            onNavigate = ::navigateTo,
                            showSecondaryBack = true
                        )

                        // Floating Frosted Glass Bottom Navigation Bar
                        GlassBottomNavBar(
                            currentDestination = currentDestination,
                            onSelectTab = ::navigateTo,
                            onOpenMore = { showMoreSheet = true },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }

    if (showMoreSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showMoreSheet = false },
            sheetState = sheetState,
            containerColor = colors.glassSurfaceElevated,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "بخش‌های LIFEOS",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
                Text(
                    text = "دسترسی سریع به تمام ابزارهای مدیریت زندگی",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                Spacer(Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                ) {
                    items(LifeOsDestination.secondaryScreens) { dest ->
                        val isSelected = currentDestination == dest
                        GlassCard(
                            accentTint = if (isSelected) colors.oliveSoftContainer else null,
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(12.dp),
                            onClick = {
                                showMoreSheet = false
                                navigateTo(dest)
                            },
                            modifier = Modifier.testTag("more_item_${dest.route}")
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) colors.oliveAccent else colors.oliveSoftContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.titleFa,
                                        tint = if (isSelected) Color.White else colors.oliveAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Text(
                                    text = dest.titleFa,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun LifeOsNavHost(
    navController: NavHostController,
    viewModel: LifeOsViewModel,
    onNavigate: (LifeOsDestination) -> Unit,
    showSecondaryBack: Boolean
) {
    val backAction: (() -> Unit)? = if (showSecondaryBack) {
        { navController.popBackStack() }
    } else null

    NavHost(
        navController = navController,
        startDestination = LifeOsDestination.DASHBOARD.route,
        enterTransition = { fadeIn(animationSpec = tween(200)) },
        exitTransition = { fadeOut(animationSpec = tween(160)) },
        popEnterTransition = { fadeIn(animationSpec = tween(200)) },
        popExitTransition = { fadeOut(animationSpec = tween(160)) }
    ) {
        composable(LifeOsDestination.DASHBOARD.route) {
            DashboardScreen(viewModel = viewModel, onNavigate = onNavigate)
        }
        composable(LifeOsDestination.PLANNER.route) {
            PlannerScreen(viewModel = viewModel)
        }
        composable(LifeOsDestination.TASKS.route) {
            TasksScreen(viewModel = viewModel)
        }
        composable(LifeOsDestination.GOALS.route) {
            GoalsScreen(viewModel = viewModel)
        }
        composable(LifeOsDestination.HABITS.route) {
            HabitsScreen(viewModel = viewModel, onBack = backAction)
        }
        composable(LifeOsDestination.CALENDAR.route) {
            CalendarScreen(viewModel = viewModel, onBack = backAction)
        }
        composable(LifeOsDestination.PROJECTS.route) {
            ProjectsScreen(viewModel = viewModel, onBack = backAction)
        }
        composable(LifeOsDestination.NOTES.route) {
            NotesScreen(viewModel = viewModel, onBack = backAction)
        }
        composable(LifeOsDestination.ANALYTICS.route) {
            AnalyticsScreen(viewModel = viewModel, onBack = backAction)
        }
        composable(LifeOsDestination.SETTINGS.route) {
            SettingsScreen(viewModel = viewModel, onBack = backAction)
        }
    }
}

@Composable
private fun GlassBottomNavBar(
    currentDestination: LifeOsDestination,
    onSelectTab: (LifeOsDestination) -> Unit,
    onOpenMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LifeOsTheme.colors
    val shape = RoundedCornerShape(24.dp)
    val isSecondaryActive = !currentDestination.isPrimaryBottomNav

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = colors.navyStrong.copy(alpha = 0.14f),
                spotColor = colors.navyStrong.copy(alpha = 0.14f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(colors.glassSurfaceElevated, colors.glassSurface)
                )
            )
            .border(width = 1.dp, color = colors.glassBorder, shape = shape)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LifeOsDestination.primaryTabs.forEach { dest ->
            val selected = currentDestination == dest
            BottomNavPillItem(
                label = dest.titleFa,
                icon = if (selected) dest.selectedIcon else dest.unselectedIcon,
                selected = selected,
                onClick = { onSelectTab(dest) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("bottom_nav_${dest.route}")
            )
        }

        BottomNavPillItem(
            label = if (isSecondaryActive) currentDestination.titleFa else "بیشتر",
            icon = if (isSecondaryActive) currentDestination.selectedIcon else Icons.Outlined.Apps,
            selected = isSecondaryActive,
            onClick = onOpenMore,
            modifier = Modifier
                .weight(1f)
                .testTag("bottom_nav_more")
        )
    }
}

@Composable
private fun BottomNavPillItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LifeOsTheme.colors
    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) colors.oliveAccent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Color.White else colors.textSecondary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Color.White else colors.textSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
