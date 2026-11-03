package com.yubera.app.di

import com.yubera.app.core.player.KodikRepository
import com.yubera.app.data.player.RemoteKodikRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerBindings {
    @Binds
    @Singleton
    abstract fun bindKodikRepository(implementation: RemoteKodikRepository): KodikRepository
}
