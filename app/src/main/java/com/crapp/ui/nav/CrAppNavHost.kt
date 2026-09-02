package com.crapp.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.crapp.ui.bowel.BowelMovementLogScreen
import com.crapp.ui.food.FoodLogScreen
import com.crapp.ui.home.HomeScreen
import com.crapp.ui.medication.MedicationLogScreen

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
                onLogBowelMovement = { navController.navigate(Routes.LOG_BOWEL_MOVEMENT) },
                onLogFood = { navController.navigate(Routes.LOG_FOOD) },
                onLogMedication = { navController.navigate(Routes.LOG_MEDICATION) }
            )
        }
        composable(Routes.LOG_BOWEL_MOVEMENT) {
            BowelMovementLogScreen(onDone = { navController.popBackStack() })
        }
        composable(Routes.LOG_FOOD) {
            FoodLogScreen(onDone = { navController.popBackStack() })
        }
        composable(Routes.LOG_MEDICATION) {
            MedicationLogScreen(onDone = { navController.popBackStack() })
        }
    }
}
