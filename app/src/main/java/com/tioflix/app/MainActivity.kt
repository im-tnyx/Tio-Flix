package com.tioflix.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.tioflix.app.core.config.AppConfig
import com.tioflix.app.core.design.TioFlixTheme
import com.tioflix.app.navigation.TioFlixNavHost
import com.tioflix.app.ui.auth.reset.ResetPasswordViewModel
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var supabaseClient: Lazy<SupabaseClient>
    private val recoveryViewModel: ResetPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleAuthIntent(intent)

        setContent {
            TioFlixTheme {
                TioFlixNavHost(recoveryViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: Intent) {
        if (!AppConfig.isSupabaseConfigured) return
        if (recoveryViewModel.handleDeepLink(intent.dataString.orEmpty())) {
            intent.data = null
        } else {
            supabaseClient.get().handleDeeplinks(intent)
        }
    }
}
