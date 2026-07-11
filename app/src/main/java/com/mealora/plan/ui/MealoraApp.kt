package com.mealora.plan.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mealora.plan.ui.navigation.Routes
import com.mealora.plan.ui.screens.DayDetailScreen
import com.mealora.plan.ui.screens.DishDetailScreen
import com.mealora.plan.ui.screens.DishEditScreen
import com.mealora.plan.ui.screens.DishLibraryScreen
import com.mealora.plan.ui.screens.HistoryScreen
import com.mealora.plan.ui.screens.HistoryWeekScreen
import com.mealora.plan.ui.screens.MealEditScreen
import com.mealora.plan.ui.screens.OnboardingScreen
import com.mealora.plan.ui.screens.SettingsScreen
import com.mealora.plan.ui.screens.ShoppingListScreen
import com.mealora.plan.ui.screens.ShoppingPreviewScreen
import com.mealora.plan.ui.screens.StatisticsScreen
import com.mealora.plan.ui.screens.TemplateEditScreen
import com.mealora.plan.ui.screens.TemplatesScreen
import com.mealora.plan.ui.screens.WeeklyBoardScreen

private data class BottomItem(val route: String, val label: String, val icon: ImageVector)

private val bottomItems = listOf(
    BottomItem(Routes.BOARD, "Week", Icons.Filled.CalendarViewWeek),
    BottomItem(Routes.DISHES, "Dishes", Icons.AutoMirrored.Filled.MenuBook),
    BottomItem(Routes.SHOPPING, "Shopping", Icons.Filled.ShoppingCart),
    BottomItem(Routes.TEMPLATES, "Templates", Icons.Filled.ViewModule),
    BottomItem(Routes.HISTORY, "History", Icons.Filled.History),
)

@Composable
fun MealoraApp() {
    val vm: PlannerViewModel = viewModel(factory = PlannerViewModel.Factory)
    val data by vm.appData.collectAsStateWithLifecycle()
    val ready by vm.isReady.collectAsStateWithLifecycle()

    when {
        !ready -> {
            // Brief gate while the first stored value loads. The system splash
            // typically covers this; a plain surface avoids an onboarding flash.
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
        }
        !data.settings.onboardingCompleted -> {
            OnboardingScreen(
                onPlanWeek = { vm.completeOnboarding() },
                onExplore = { vm.completeOnboarding() },
            )
        }
        else -> MainScaffold(vm)
    }
}

@Composable
private fun MainScaffold(vm: PlannerViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = Routes.topLevel.any { currentRoute == it }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val destination = backStackEntry?.destination
                    bottomItems.forEach { item ->
                        val selected = destination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AppNavHost(vm = vm, navController = navController)
        }
    }
}

@Composable
private fun AppNavHost(
    vm: PlannerViewModel,
    navController: androidx.navigation.NavHostController,
) {
    NavHost(navController = navController, startDestination = Routes.BOARD) {
        composable(Routes.BOARD) { WeeklyBoardScreen(vm = vm, navController = navController) }
        composable(Routes.DISHES) { DishLibraryScreen(vm = vm, navController = navController) }
        composable(Routes.SHOPPING) { ShoppingListScreen(vm = vm, navController = navController) }
        composable(Routes.TEMPLATES) { TemplatesScreen(vm = vm, navController = navController) }
        composable(Routes.HISTORY) { HistoryScreen(vm = vm, navController = navController) }
        composable(Routes.STATS) { StatisticsScreen(vm = vm, navController = navController) }
        composable(Routes.SETTINGS) { SettingsScreen(vm = vm, navController = navController) }

        composable(
            route = "${Routes.DAY}?${Routes.ARG_DATE}={${Routes.ARG_DATE}}",
            arguments = listOf(navArgument(Routes.ARG_DATE) { type = NavType.StringType; defaultValue = "" }),
        ) { entry ->
            DayDetailScreen(
                vm = vm,
                navController = navController,
                dateArg = entry.arguments?.getString(Routes.ARG_DATE).orEmpty(),
            )
        }

        composable(
            route = "${Routes.MEAL_EDIT}?${Routes.ARG_DATE}={${Routes.ARG_DATE}}&${Routes.ARG_SLOT}={${Routes.ARG_SLOT}}&${Routes.ARG_ENTRY_ID}={${Routes.ARG_ENTRY_ID}}",
            arguments = listOf(
                navArgument(Routes.ARG_DATE) { type = NavType.StringType; defaultValue = "" },
                navArgument(Routes.ARG_SLOT) { type = NavType.StringType; defaultValue = "" },
                navArgument(Routes.ARG_ENTRY_ID) { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            MealEditScreen(
                vm = vm,
                navController = navController,
                dateArg = entry.arguments?.getString(Routes.ARG_DATE).orEmpty(),
                slotArg = entry.arguments?.getString(Routes.ARG_SLOT).orEmpty(),
                entryIdArg = entry.arguments?.getString(Routes.ARG_ENTRY_ID).orEmpty(),
            )
        }

        composable(
            route = "${Routes.DISH_EDIT}?${Routes.ARG_DISH_ID}={${Routes.ARG_DISH_ID}}",
            arguments = listOf(navArgument(Routes.ARG_DISH_ID) { type = NavType.StringType; defaultValue = "" }),
        ) { entry ->
            DishEditScreen(
                vm = vm,
                navController = navController,
                dishIdArg = entry.arguments?.getString(Routes.ARG_DISH_ID).orEmpty(),
            )
        }

        composable(
            route = "${Routes.DISH_DETAIL}?${Routes.ARG_DISH_ID}={${Routes.ARG_DISH_ID}}",
            arguments = listOf(navArgument(Routes.ARG_DISH_ID) { type = NavType.StringType; defaultValue = "" }),
        ) { entry ->
            DishDetailScreen(
                vm = vm,
                navController = navController,
                dishIdArg = entry.arguments?.getString(Routes.ARG_DISH_ID).orEmpty(),
            )
        }

        composable(Routes.SHOPPING_PREVIEW) { ShoppingPreviewScreen(vm = vm, navController = navController) }

        composable(
            route = "${Routes.TEMPLATE_EDIT}?${Routes.ARG_TEMPLATE_ID}={${Routes.ARG_TEMPLATE_ID}}",
            arguments = listOf(navArgument(Routes.ARG_TEMPLATE_ID) { type = NavType.StringType; defaultValue = "" }),
        ) { entry ->
            TemplateEditScreen(
                vm = vm,
                navController = navController,
                templateIdArg = entry.arguments?.getString(Routes.ARG_TEMPLATE_ID).orEmpty(),
            )
        }

        composable(
            route = "${Routes.HISTORY_WEEK}?${Routes.ARG_WEEK_START}={${Routes.ARG_WEEK_START}}",
            arguments = listOf(navArgument(Routes.ARG_WEEK_START) { type = NavType.StringType; defaultValue = "" }),
        ) { entry ->
            HistoryWeekScreen(
                vm = vm,
                navController = navController,
                weekStartArg = entry.arguments?.getString(Routes.ARG_WEEK_START).orEmpty(),
            )
        }
    }
}
