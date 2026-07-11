package com.tioflix.app.ui.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.tioflix.app.domain.usecase.HasSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val hasSession: HasSessionUseCase
) : ViewModel() {
    fun isLoggedIn(): Boolean = hasSession()
}

@Composable
fun SplashRoute(
    onAuthenticated: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        if (viewModel.isLoggedIn()) onAuthenticated() else onLoggedOut()
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
