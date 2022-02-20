package ren.imyan.app_tracker.base

sealed class BaseLoad<out T> {
    object Loading : BaseLoad<Nothing>()
    data class Success<out T>(val data: T) : BaseLoad<T>()
    data class Error(val error: Throwable) : BaseLoad<Nothing>()
}