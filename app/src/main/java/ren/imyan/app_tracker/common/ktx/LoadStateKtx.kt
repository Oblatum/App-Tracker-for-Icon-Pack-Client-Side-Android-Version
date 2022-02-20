package ren.imyan.app_tracker.common.ktx

import com.drake.statelayout.StateLayout
import ren.imyan.app_tracker.base.BaseLoad

fun <T> StateLayout?.updateState(
    loadState: BaseLoad<T>?, onError: ((Throwable?) -> Unit)? = null,
    onLoading: (() -> Unit)? = null,
    onSuccess: ((T) -> Unit)? = null
) {
    this ?: return
    loadState ?: return
    when (loadState) {
        is BaseLoad.Error -> {
            showError()
            onError?.invoke(loadState.error)
        }
        BaseLoad.Loading -> {
            showLoading()
            onLoading?.invoke()
        }
        is BaseLoad.Success -> {
            showContent()
            onSuccess?.invoke(loadState.data)
        }
    }
}