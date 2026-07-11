package com.tioflix.app.di

import com.tioflix.app.data.auth.SupabaseAuthRepository
import com.tioflix.app.data.catalog.SupabaseCatalogRepository
import com.tioflix.app.data.profile.SupabaseProfileRepository
import com.tioflix.app.domain.repository.AuthRepository
import com.tioflix.app.domain.repository.CatalogRepository
import com.tioflix.app.domain.repository.ProfileRepository
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
}
