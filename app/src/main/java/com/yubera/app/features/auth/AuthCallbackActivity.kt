package com.yubera.app.features.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.yubera.app.MainActivity
import com.yubera.app.core.session.NativeAuthRepository
import com.yubera.app.features.library.LibrarySyncScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AuthCallbackActivity : ComponentActivity() {
    @Inject lateinit var authRepository: NativeAuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val code = intent?.data?.getQueryParameter("code")
        val state = intent?.data?.getQueryParameter("state")
        lifecycleScope.launch {
            try {
                require(!code.isNullOrBlank() && !state.isNullOrBlank()) {
                    "В ответе авторизации отсутствуют обязательные параметры."
                }
                authRepository.completeCallback(code, state)
                LibrarySyncScheduler.enqueueNow(this@AuthCallbackActivity)
            } catch (error: Exception) {
                Toast.makeText(
                    this@AuthCallbackActivity,
                    error.message ?: "Не удалось завершить вход через Telegram.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                startActivity(
                    Intent(this@AuthCallbackActivity, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                finish()
            }
        }
    }
}
