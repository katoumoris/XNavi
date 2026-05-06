package com.xnavi.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xnavi.app.ui.screens.MainScreen
import com.xnavi.app.ui.screens.NavigationScreen
import com.xnavi.app.ui.screens.RoutePlanScreen
import com.xnavi.app.ui.screens.SearchScreen

@Composable
fun XNaviApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") { backStackEntry ->
            MainScreen(navController = navController)
        }
        composable("search") {
            SearchScreen(navController = navController)
        }
        composable(
            "route_plan/{origin}/{dest}",
            arguments = listOf(
                navArgument("origin") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                },
                navArgument("dest") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            )
        ) { backStackEntry ->
            RoutePlanScreen(navController = navController)
        }
        composable("navigation") {
            NavigationScreen(navController = navController)
        }
    }
}
