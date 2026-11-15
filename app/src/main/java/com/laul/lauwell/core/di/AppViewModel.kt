package com.laul.lauwell.core.di

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.laul.lauwell.LauWellApplication

/**
 * Gets (or creates) a ViewModel whose dependencies come from the [AppContainer]:
 *
 * ```
 * val vm = appViewModel { SignInViewModel(authRepository) }
 * ```
 *
 * The lambda runs with the container as receiver, and only when the ViewModel doesn't exist yet;
 * on recomposition or rotation the existing instance is returned, as with `viewModel()`. A [key]
 * keeps one per value on the same screen (each metric shown in Tracker's list + detail).
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    crossinline create: AppContainer.() -> VM,
): VM = viewModel(
    key = key,
    factory = viewModelFactory {
        initializer {
            val app = this[APPLICATION_KEY] as LauWellApplication
            app.container.create()
        }
    },
)
