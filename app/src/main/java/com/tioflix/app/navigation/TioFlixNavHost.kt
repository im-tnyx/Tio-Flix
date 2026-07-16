package com.tioflix.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tioflix.app.ui.auth.forgot.ForgotPasswordRoute
import com.tioflix.app.ui.auth.login.LoginRoute
import com.tioflix.app.ui.auth.signup.SignupRoute
import com.tioflix.app.ui.detail.ContentDetailRoute
import com.tioflix.app.ui.home.HomeRoute
import com.tioflix.app.ui.player.PlayerRoute
import com.tioflix.app.ui.search.SearchRoute
import com.tioflix.app.ui.splash.SplashRoute

private object Destinations {
    const val Splash = "splash"
    const val Login = "auth/login"
    const val Signup = "auth/signup"
    const val ForgotPassword = "auth/forgot-password"
    const val Home = "home"
    const val Search = "search"
    const val ContentDetail = "content/{contentId}"
    const val Player = "player/{contentId}?episodeId={episodeId}"

    fun contentDetail(contentId: String) = "content/$contentId"
    fun player(contentId: String, episodeId: String?) =
        "player/$contentId?episodeId=${episodeId.orEmpty()}"
}

@Composable
fun TioFlixNavHost() {
    val navController = rememberNavController()

    fun navigateClearingAuth(destination: String) {
        navController.navigate(destination) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = Destinations.Splash) {
        composable(Destinations.Splash) {
            SplashRoute(
                onAuthenticated = { navigateClearingAuth(Destinations.Home) },
                onLoggedOut = { navigateClearingAuth(Destinations.Login) }
            )
        }

        composable(Destinations.Login) {
            LoginRoute(
                onLoginSuccess = { navigateClearingAuth(Destinations.Home) },
                onSignupClick = { navController.navigate(Destinations.Signup) },
                onForgotPasswordClick = { navController.navigate(Destinations.ForgotPassword) }
            )
        }

        composable(Destinations.Signup) {
            SignupRoute(onCompleted = { navController.popBackStack() })
        }

        composable(Destinations.ForgotPassword) {
            ForgotPasswordRoute()
        }

        composable(Destinations.Home) {
            HomeRoute(
                onLoggedOut = { navigateClearingAuth(Destinations.Login) },
                onSearchClick = { navController.navigate(Destinations.Search) },
                onContentClick = { contentId ->
                    navController.navigate(Destinations.contentDetail(contentId))
                }
            )
        }

        composable(Destinations.Search) {
            SearchRoute(
                onBack = { navController.popBackStack() },
                onContentClick = { contentId ->
                    navController.navigate(Destinations.contentDetail(contentId))
                }
            )
        }

        composable(
            route = Destinations.ContentDetail,
            arguments = listOf(navArgument("contentId") { type = NavType.StringType })
        ) {
            ContentDetailRoute(
                onBack = { navController.popBackStack() },
                onOpenPlayer = { contentId, episodeId ->
                    navController.navigate(Destinations.player(contentId, episodeId))
                }
            )
        }

        composable(
            route = Destinations.Player,
            arguments = listOf(
                navArgument("contentId") { type = NavType.StringType },
                navArgument("episodeId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            PlayerRoute(onBack = { navController.popBackStack() })
        }
    }
}
