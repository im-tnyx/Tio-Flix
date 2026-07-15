package com.tioflix.app.ui.player

import com.tioflix.app.domain.model.SeriesEpisode
import com.tioflix.app.domain.repository.CatalogRepository
import javax.inject.Inject

class NextEpisodeResolver @Inject constructor(
    private val catalogRepository: CatalogRepository
) {
    suspend operator fun invoke(contentId: String, episodeId: String): SeriesEpisode? {
        val episodes = catalogRepository.getSeriesSeasons(contentId)
            .getOrDefault(emptyList())
            .sortedBy { it.seasonNumber }
            .flatMap { season -> season.episodes.sortedBy { it.episodeNumber } }
        val currentIndex = episodes.indexOfFirst { it.id == episodeId }
        return if (currentIndex >= 0) episodes.getOrNull(currentIndex + 1) else null
    }
}
