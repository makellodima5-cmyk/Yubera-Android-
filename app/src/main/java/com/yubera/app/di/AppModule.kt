package com.yubera.app.di

import com.yubera.app.core.session.SessionStateProvider
import com.yubera.app.core.session.UnconfiguredSessionStateProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindSessionStateProvider(
        implementation: UnconfiguredSessionStateProvider
    ): SessionStateProvider
}
