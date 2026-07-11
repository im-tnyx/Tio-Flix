package com.tioflix.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tioflix.app.ui.home.HomeRoute

private object Destinations {
    const val Home = "home"
}

@Composable
fun TioFlixNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destinations.Home
    ) {
        composable(Destinations.Home) {
            HomeRoute()
        }
    }
}
