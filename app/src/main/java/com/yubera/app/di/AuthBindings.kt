package com.yubera.app.di

import com.yubera.app.core.session.AndroidKeystoreTokenStore
import com.yubera.app.core.session.SecureTokenStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthBindings {
    @Binds
    @Singleton
    abstract fun bindSecureTokenStore(implementation: AndroidKeystoreTokenStore): SecureTokenStore
}
