package com.yubera.app.core.session

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteSessionStateProvider @Inject constructor(
    private val authRepository: NativeAuthRepository
) : SessionStateProvider {
    override suspend fun hasRestorableSession(): Boolean = authRepository.hasRestorableSession()
}
