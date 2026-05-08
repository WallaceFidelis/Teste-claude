package com.nfscan.ui.navigation

import androidx.navigation.NavHostController
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nfscan.ui.home.HomeScreen
import com.nfscan.ui.result.ResultScreen
import com.nfscan.ui.scanning.ScanningScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home,
    ) {
        composable<Screen.Home> {
            HomeScreen(
                onFileSelected = { filePath ->
                    navController.navigate(Screen.Scanning(filePath))
                }
            )
        }

        composable<Screen.Scanning> { backStackEntry ->
            val screen = backStackEntry.toRoute<Screen.Scanning>()
            ScanningScreen(
                filePath = screen.filePath,
                onScanComplete = {
                    navController.navigate(Screen.Result) {
                        popUpTo(Screen.Home)
                    }
                },
                onError = { navController.popBackStack() },
            )
        }

        composable<Screen.Result> {
            ResultScreen(
                onScanAgain = {
                    navController.popBackStack(Screen.Home, inclusive = false)
                }
            )
        }
    }
}
