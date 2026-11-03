package com.yubera.app.di

import com.yubera.app.core.domain.library.PersonalLibraryRepository
import com.yubera.app.data.library.RoomPersonalLibraryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryBindings {
    @Binds
    @Singleton
    abstract fun bindPersonalLibraryRepository(
        implementation: RoomPersonalLibraryRepository
    ): PersonalLibraryRepository
}
