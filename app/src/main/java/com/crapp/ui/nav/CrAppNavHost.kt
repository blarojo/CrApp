package com.crapp.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.crapp.ui.bowel.BowelMovementLogScreen
import com.crapp.ui.export.ExportScreen
import com.crapp.ui.food.FoodLogScreen
import com.crapp.ui.foodcatalog.FoodCatalogScreen
import com.crapp.ui.history.HistoryScreen
import com.crapp.ui.home.HomeScreen
import com.crapp.ui.insights.InsightsScreen
import com.crapp.ui.medication.MedicationLogScreen
import com.crapp.ui.settings.SettingsScreen

@Composable
fun CrAppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onLogBowelMovement = { navController.navigate(Routes.logBowelMovement()) },
                onLogFood = { navController.navigate(Routes.logFood()) },
                onLogMedication = { navController.navigate(Routes.logMedication()) },
                onViewHistory = { navController.navigate(Routes.HISTORY) },
                onExport = { navController.navigate(Routes.EXPORT) },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onManageFoodCatalog = { navController.navigate(Routes.FOOD_CATALOG) },
                onViewInsights = { navController.navigate(Routes.INSIGHTS) }
            )
        }
        composable(Routes.FOOD_CATALOG) {
            FoodCatalogScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.INSIGHTS) {
            InsightsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onEditBowelMovement = { id -> navController.navigate(Routes.logBowelMovement(id)) },
                onEditFood = { id -> navController.navigate(Routes.logFood(id)) },
                onEditMedication = { id -> navController.navigate(Routes.logMedication(id)) },
                onExport = { navController.navigate(Routes.EXPORT) }
            )
        }
        composable(Routes.EXPORT) {
            ExportScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Routes.LOG_BOWEL_MOVEMENT_PATTERN,
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })
        ) {
            BowelMovementLogScreen(onDone = { navController.popBackStack() })
        }
        composable(
            Routes.LOG_FOOD_PATTERN,
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })
        ) {
            FoodLogScreen(onDone = { navController.popBackStack() })
        }
        composable(
            Routes.LOG_MEDICATION_PATTERN,
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })
        ) {
            MedicationLogScreen(onDone = { navController.popBackStack() })
        }
    }
}
