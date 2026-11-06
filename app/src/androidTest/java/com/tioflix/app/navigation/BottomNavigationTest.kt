package com.tioflix.app.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BottomNavigationTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var navController: NavHostController

    @Test fun searchStateSurvivesRepeatedTabSwitchesAfterSplash() {
        showNavigation(startDestination = "splash")
        composeRule.onNodeWithText("Authenticated").performClick()

        composeRule.runOnIdle {
            assertEquals("home", navController.currentDestination?.route)
            navController.getBackStackEntry("home")
        }
        composeRule.onNodeWithText("Search tab").performClick()
        composeRule.onNodeWithTag("search-query").performTextInput("interstellar")

        repeat(3) {
            composeRule.onNodeWithText("Home tab").performClick()
            composeRule.onNodeWithText("Search tab").performClick()
            composeRule.onNodeWithTag("search-query").assertTextEquals("interstellar")
        }

        composeRule.onNodeWithText("Home tab").performClick()
        composeRule.runOnIdle {
            assertEquals("home", navController.currentDestination?.route)
            // A redundant Search/back-stack entry must not remain behind Home.
            assertFalse(navController.popBackStack())
        }
    }

    @Test fun recoveryToLoginKeepsAuthClearedAndHomeAnchored() {
        showNavigation(startDestination = "auth/reset-password")
        composeRule.onNodeWithText("Complete recovery").performClick()
        composeRule.onNodeWithText("Sign in").performClick()
        composeRule.onNodeWithText("Search tab").performClick()
        composeRule.onNodeWithText("Home tab").performClick()

        composeRule.runOnIdle {
            assertEquals("home", navController.currentDestination?.route)
            navController.getBackStackEntry("home")
            for (route in listOf("auth/reset-password", "auth/login")) {
                try {
                    navController.getBackStackEntry(route)
                    fail("Auth route must be cleared: $route")
                } catch (_: IllegalArgumentException) {
                    // Expected after navigateClearingAuth from the original app.
                }
            }
            assertFalse(navController.popBackStack())
        }
    }

    @Composable
    private fun TestSearchScreen(controller: NavHostController) {
        var query by rememberSaveable { mutableStateOf("") }
        Column {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.testTag("search-query")
            )
            Button(onClick = { controller.navigateToBottomDestination("home") }) {
                Text("Home tab")
            }
        }
    }

    private fun showNavigation(startDestination: String) {
        composeRule.setContent {
            val controller = rememberNavController()
            SideEffect { navController = controller }
            NavHost(navController = controller, startDestination = startDestination) {
                composable("splash") {
                    Button(onClick = { controller.navigateClearingAuthForTest("home") }) {
                        Text("Authenticated")
                    }
                }
                composable("auth/reset-password") {
                    Button(onClick = { controller.navigateClearingAuthForTest("auth/login") }) {
                        Text("Complete recovery")
                    }
                }
                composable("auth/login") {
                    Button(onClick = { controller.navigateClearingAuthForTest("home") }) {
                        Text("Sign in")
                    }
                }
                composable("home") {
                    Button(onClick = { controller.navigateToBottomDestination("search") }) {
                        Text("Search tab")
                    }
                }
                composable("search") { TestSearchScreen(controller) }
            }
        }
    }

    // Same auth stack-clearing options as TioFlixNavHost; auth production code is unchanged.
    private fun NavHostController.navigateClearingAuthForTest(destination: String) {
        navigate(destination) {
            popUpTo(graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }
}
