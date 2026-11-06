package com.tioflix.app.di

import com.tioflix.app.data.auth.SupabaseAuthRepository
import com.tioflix.app.data.auth.SupabasePasswordRecoveryRepository
import com.tioflix.app.domain.repository.PasswordRecoveryRepository
import com.tioflix.app.data.catalog.SupabaseCatalogRepository
import com.tioflix.app.data.favorites.SupabaseFavoritesRepository
import com.tioflix.app.data.history.SupabaseWatchHistoryRepository
import com.tioflix.app.data.playback.BackendPlaybackRepository
import com.tioflix.app.data.profile.SupabaseProfileRepository
import com.tioflix.app.domain.repository.AuthRepository
import com.tioflix.app.domain.repository.CatalogRepository
import com.tioflix.app.domain.repository.FavoritesRepository
import com.tioflix.app.domain.repository.PlaybackRepository
import com.tioflix.app.domain.repository.ProfileRepository
import com.tioflix.app.domain.repository.WatchHistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindPasswordRecoveryRepository(
        implementation: SupabasePasswordRecoveryRepository
    ): PasswordRecoveryRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        implementation: SupabaseAuthRepository
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        implementation: SupabaseProfileRepository
    ): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindCatalogRepository(
        implementation: SupabaseCatalogRepository
    ): CatalogRepository

    @Binds
    @Singleton
    abstract fun bindPlaybackRepository(
        implementation: BackendPlaybackRepository
    ): PlaybackRepository

    @Binds
    @Singleton
    abstract fun bindWatchHistoryRepository(
        implementation: SupabaseWatchHistoryRepository
    ): WatchHistoryRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(
        implementation: SupabaseFavoritesRepository
    ): FavoritesRepository
}
