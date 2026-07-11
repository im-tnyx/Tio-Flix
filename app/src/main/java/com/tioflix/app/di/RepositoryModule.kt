package com.tioflix.app.di

import com.tioflix.app.data.auth.SupabaseAuthRepository
import com.tioflix.app.domain.repository.AuthRepository
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
}
