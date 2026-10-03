package com.laul.lauwell.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laul.lauwell.core.auth.AuthRepository
import kotlinx.coroutines.launch

class SettingsViewModel(private val authRepository: AuthRepository) : ViewModel() {

    /** The session ends and [com.laul.lauwell.core.auth.ui.AuthGate] shows sign-in on its own. */
    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
