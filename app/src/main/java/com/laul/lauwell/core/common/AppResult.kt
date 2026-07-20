package com.laul.lauwell.core.common

/**
 * Shared result wrapper for repository/use-case return types across all feature modules.
 *
 * Every feature (Health, Medication, Ostomy, Glycemia, ...) should return this from
 * its repository layer instead of throwing or returning nullable types directly, so the UI
 * layer can handle loading/error/success uniformly regardless of which module it's rendering.
 */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(val throwable: Throwable, val message: String? = null) : AppResult<Nothing>
    data object Loading : AppResult<Nothing>
}
