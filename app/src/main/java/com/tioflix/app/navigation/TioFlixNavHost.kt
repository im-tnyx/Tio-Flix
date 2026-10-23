package com.tioflix.app.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tioflix.app.ui.auth.reset.ResetPasswordRoute
import com.tioflix.app.ui.auth.reset.ResetPasswordViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.tioflix.app.core.config.AppConfig
import com.tioflix.app.ui.auth.checkemail.CheckEmailRoute
import com.tioflix.app.ui.auth.forgot.ForgotPasswordRoute
import com.tioflix.app.ui.auth.login.LoginRoute
import com.tioflix.app.ui.auth.signup.SignupRoute
import com.tioflix.app.ui.configuration.ConfigurationRequiredScreen
import com.tioflix.app.ui.detail.ContentDetailRoute
import com.tioflix.app.ui.home.HomeRoute
import com.tioflix.app.ui.mylist.MyListRoute
import com.tioflix.app.ui.player.PlayerRoute
import com.tioflix.app.ui.search.SearchRoute
import com.tioflix.app.ui.settings.SettingsRoute
import com.tioflix.app.ui.splash.SplashRoute

private object Destinations {
    const val Splash = "splash"
    const val Login = "auth/login?email={email}&message={message}"
    const val Signup = "auth/signup"
    const val CheckEmail = "auth/check-email?email={email}"
    const val ForgotPassword = "auth/forgot-password"
    const val ResetPassword = "auth/reset-password"
    const val Home = "home"
    const val MyList = "my-list"
    const val Settings = "settings"
    const val Search = "search"
    const val ContentDetail = "content/{contentId}"
    const val Player = "player/{contentId}?episodeId={episodeId}"

    fun login(email: String = "", message: String = "") =
        "auth/login?email=${Uri.encode(email)}&message=${Uri.encode(message)}"
    fun checkEmail(email: String) = "auth/check-email?email=${Uri.encode(email)}"
    fun contentDetail(contentId: String) = "content/$contentId"
    fun player(contentId: String, episodeId: String?) =
        "player/$contentId?episodeId=${episodeId.orEmpty()}"
}

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomDestinations = listOf(
    BottomDestination(Destinations.Home, "Home", Icons.Filled.Home),
    BottomDestination(Destinations.Search, "Search", Icons.Filled.Search),
    BottomDestination(Destinations.MyList, "My List", Icons.Filled.Bookmark),
    BottomDestination(Destinations.Settings, "Settings", Icons.Filled.Person)
)

@Composable
fun TioFlixNavHost(recoveryViewModel: ResetPasswordViewModel) {
    if (!AppConfig.isSupabaseConfigured) {
        ConfigurationRequiredScreen()
        return
    }

    val navController = rememberNavController()
    val recoveryState by recoveryViewModel.uiState.collectAsStateWithLifecycle()
    val initialDestination = remember {
        if (recoveryState.isActive) Destinations.ResetPassword else Destinations.Splash
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = bottomDestinations.any { it.route == currentRoute }

    fun navigateClearingAuth(destination: String) {
        navController.navigate(destination) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun navigateToBottomDestination(destination: String) {
        navController.navigate(destination) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(recoveryState.isActive) {
        if (recoveryState.isActive) navigateClearingAuth(Destinations.ResetPassword)
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = { navigateToBottomDestination(destination.route) },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label
                                )
                            },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            modifier = Modifier.padding(innerPadding),
            navController = navController,
            startDestination = initialDestination
        ) {
            composable(Destinations.Splash) {
                SplashRoute(
                    onAuthenticated = { if (!recoveryState.isActive) navigateClearingAuth(Destinations.Home) },
                    onLoggedOut = { if (!recoveryState.isActive) navigateClearingAuth(Destinations.login()) }
                )
            }

            composable(
                route = Destinations.Login,
                arguments = listOf(
                    navArgument("email") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("message") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) {
                LoginRoute(
                    onLoginSuccess = { if (!recoveryState.isActive) navigateClearingAuth(Destinations.Home) },
                    onSignupClick = { navController.navigate(Destinations.Signup) },
                    onForgotPasswordClick = { navController.navigate(Destinations.ForgotPassword) }
                )
            }

            composable(Destinations.Signup) {
                SignupRoute(
                    onCompleted = { email ->
                        navController.navigate(Destinations.checkEmail(email)) {
                            popUpTo(Destinations.Signup) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = Destinations.CheckEmail,
                arguments = listOf(
                    navArgument("email") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                CheckEmailRoute(
                    email = backStackEntry.arguments?.getString("email").orEmpty(),
                    onBackToLogin = { email ->
                        navigateClearingAuth(
                            Destinations.login(
                                email = email,
                                message = "Email verify ho gaya ho to apna password dal kar continue karein."
                            )
                        )
                    },
                    onVerified = { if (!recoveryState.isActive) navigateClearingAuth(Destinations.Home) }
                )
            }

            composable(Destinations.ForgotPassword) {
                ForgotPasswordRoute()
            }

            composable(Destinations.ResetPassword) {
                ResetPasswordRoute(recoveryViewModel) { message ->
                    navigateClearingAuth(Destinations.login(message = message))
                }
            }

            composable(Destinations.Home) {
                HomeRoute(
                    onContentClick = { contentId ->
                        navController.navigate(Destinations.contentDetail(contentId))
                    }
                )
            }

            composable(Destinations.Search) {
                SearchRoute(
                    onBack = null,
                    onContentClick = { contentId ->
                        navController.navigate(Destinations.contentDetail(contentId))
                    },
                    showBackButton = false
                )
            }

            composable(Destinations.MyList) {
                MyListRoute(
                    onContentClick = { contentId ->
                        navController.navigate(Destinations.contentDetail(contentId))
                    }
                )
            }

            composable(Destinations.Settings) {
                SettingsRoute(
                    onLoggedOut = { navigateClearingAuth(Destinations.login()) }
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
}
