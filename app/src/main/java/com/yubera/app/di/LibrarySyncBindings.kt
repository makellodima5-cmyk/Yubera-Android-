package com.yubera.app.di

import com.yubera.app.features.library.LibrarySyncTrigger
import com.yubera.app.features.library.WorkManagerLibrarySyncTrigger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LibrarySyncBindings {
    @Binds
    @Singleton
    abstract fun bindLibrarySyncTrigger(implementation: WorkManagerLibrarySyncTrigger): LibrarySyncTrigger
}
