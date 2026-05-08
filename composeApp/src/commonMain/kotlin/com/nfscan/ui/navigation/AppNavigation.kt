package com.nfscan.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nfscan.ui.home.HomeScreen
import com.nfscan.ui.result.ResultScreen
import com.nfscan.ui.scanning.ScanningScreen
import com.nfscan.viewmodel.ReceiptViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppNavigation(navController: NavHostController) {
    // Single ViewModel instance shared across all screens in this graph
    val viewModel: ReceiptViewModel = koinViewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Home,
    ) {
        composable<Screen.Home> {
            HomeScreen(
                onFileReady = { bytes, isImage ->
                    viewModel.setPendingSource(bytes, isImage)
                    navController.navigate(Screen.Scanning)
                },
            )
        }

        composable<Screen.Scanning> {
            ScanningScreen(
                viewModel     = viewModel,
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
                viewModel  = viewModel,
                onScanAgain = {
                    viewModel.reset()
                    navController.popBackStack(Screen.Home, inclusive = false)
                },
            )
        }
    }
}
