package com.tioflix.app.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tioflix.app.core.design.TioFlixTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun logoutRemainsReachableWithShortViewportAndLargeFonts() {
        var logoutClicks = 0
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, 1.6f)
            ) {
                TioFlixTheme {
                    SettingsScreen(
                        state = SettingsUiState(
                            displayName = "Example profile",
                            email = "sample.viewer@example.com"
                        ),
                        onAction = { action ->
                            if (action == SettingsAction.LogoutClicked) logoutClicks++
                        },
                        modifier = Modifier.width(320.dp).height(280.dp)
                    )
                }
            }
        }

        composeRule.onNodeWithText("Logout")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle { assertEquals(1, logoutClicks) }
    }
}
