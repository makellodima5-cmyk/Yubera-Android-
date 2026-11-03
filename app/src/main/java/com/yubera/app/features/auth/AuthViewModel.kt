package com.yubera.app.features.auth

import androidx.lifecycle.ViewModel
import com.yubera.app.core.session.NativeAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: NativeAuthRepository
) : ViewModel() {
    fun createAuthorizationUrl(flow: String, privacyConsent: Boolean = false, termsAccepted: Boolean = false): String =
        repository.createAuthorizationUrl(flow, privacyConsent, termsAccepted)
}
