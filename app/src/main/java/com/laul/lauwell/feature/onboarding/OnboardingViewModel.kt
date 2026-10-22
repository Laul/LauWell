package com.laul.lauwell.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laul.lauwell.core.settings.Onboarding
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Which onboarding steps the navigation host still has to show. */
class OnboardingViewModel(onboarding: Onboarding) : ViewModel() {

    /** Null until read, so the grant screen never flashes for someone who already answered it. */
    val healthConnectPromptSeen: StateFlow<Boolean?> = onboarding.healthConnectPromptSeen
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
