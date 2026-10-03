package com.laul.lauwell.core.auth.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laul.lauwell.core.auth.AuthException
import com.laul.lauwell.core.auth.AuthRepository
import com.laul.lauwell.core.common.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What the sign-in screen shows. */
sealed interface SignInUiState {
    data object Idle : SignInUiState
    data object Loading : SignInUiState
    data class Failed(val error: AuthException) : SignInUiState
}

/**
 * Drives [SignInScreen]. On success it does nothing visible: [AuthRepository.session] changes and
 * [AuthGate] swaps to the signed-in content on its own.
 */
class SignInViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow<SignInUiState>(SignInUiState.Idle)
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    /** @param activityContext the current Activity, which the Google account sheet attaches to. */
    fun signIn(activityContext: Context) {
        if (_state.value == SignInUiState.Loading) return // Ignore double taps.
        _state.value = SignInUiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = authRepository.signIn(activityContext)) {
                is AppResult.Success, AppResult.Loading -> SignInUiState.Idle
                is AppResult.Error -> when (val error = result.throwable) {
                    is AuthException.Cancelled -> SignInUiState.Idle // User closed the sheet.
                    is AuthException -> SignInUiState.Failed(error)
                    else -> SignInUiState.Failed(AuthException.Unknown(error))
                }
            }
        }
    }
}
