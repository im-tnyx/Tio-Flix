package com.tioflix.app.ui.mylist

import com.tioflix.app.domain.model.ContentItem
import com.tioflix.app.domain.model.ContentType
import com.tioflix.app.domain.repository.FavoritesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyListViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun refreshReplacesRemovedFavoritesWithEmptyState() = runTest(dispatcher) {
        val repository = FakeFavoritesRepository(listOf(movie()))
        val viewModel = MyListViewModel(repository)

        // Initial data is requested by the Route's ON_RESUME lifecycle event.
        viewModel.onAction(MyListAction.RefreshRequested)
        runCurrent()
        assertEquals(1, repository.reads)
        assertEquals(listOf("saved-movie"), viewModel.uiState.value.items.map { it.id })

        repository.savedItems = emptyList() // Detail screen removed the favorite.
        viewModel.onAction(MyListAction.RefreshRequested)
        runCurrent()

        assertEquals(2, repository.reads)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.items.isEmpty())
        assertEquals(null, viewModel.uiState.value.errorMessage)
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
