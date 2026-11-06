package com.tioflix.app.ui.mylist

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tioflix.app.core.design.TioFlixTheme
import com.tioflix.app.domain.model.ContentItem
import com.tioflix.app.domain.model.ContentType
import com.tioflix.app.domain.repository.FavoritesRepository
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MyListRouteTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test fun resumingAfterFavoriteRemovalShowsUpdatedEmptyList() {
        val repository = FakeFavoritesRepository(listOf(movie()))
        val viewModel = MyListViewModel(repository)
        val lifecycleOwner = ManualLifecycleOwner()

        composeRule.runOnUiThread {
            lifecycleOwner.send(Lifecycle.Event.ON_CREATE)
            lifecycleOwner.send(Lifecycle.Event.ON_START)
        }
        composeRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                TioFlixTheme {
                    MyListRoute(onContentClick = {}, viewModel = viewModel)
                }
            }
        }

        composeRule.runOnIdle { lifecycleOwner.send(Lifecycle.Event.ON_RESUME) }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Saved movie").fetchSemanticsNodes().isNotEmpty()
        }

        // Simulate opening Content Detail, removing its favorite and navigating Back.
        composeRule.runOnIdle {
            lifecycleOwner.send(Lifecycle.Event.ON_PAUSE)
            repository.savedItems = emptyList()
            lifecycleOwner.send(Lifecycle.Event.ON_RESUME)
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Your list is empty.").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Saved movie").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(2, repository.reads) }
    }

    private class ManualLifecycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle = registry
        fun send(event: Lifecycle.Event) { registry.handleLifecycleEvent(event) }
    }

    private fun movie() = ContentItem(
        id = "saved-movie",
        type = ContentType.MOVIE,
        title = "Saved movie",
        description = null,
        posterUrl = null,
        backdropUrl = null,
        releaseYear = null,
        durationMinutes = null,
        totalSeasons = null,
        maturityRating = null,
        language = null,
        isFeatured = false
    )

    private class FakeFavoritesRepository(
        var savedItems: List<ContentItem>
    ) : FavoritesRepository {
        var reads = 0
        override suspend fun isFavorite(contentId: String) = Result.success(
            savedItems.any { it.id == contentId }
        )
        override suspend fun setFavorite(contentId: String, isFavorite: Boolean) =
            Result.success(Unit)
        override suspend fun getFavorites(limit: Int): Result<List<ContentItem>> {
            reads++
            return Result.success(savedItems.take(limit))
        }
    }
}
