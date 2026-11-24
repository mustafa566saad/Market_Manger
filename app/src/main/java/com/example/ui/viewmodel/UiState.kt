package com.example.ui.viewmodel

/**
 * Crash-proof sealed interface representing screen UI states across Mohsen Market.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
