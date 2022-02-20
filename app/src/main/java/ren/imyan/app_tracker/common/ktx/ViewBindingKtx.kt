package ren.imyan.app_tracker.common.ktx

import android.app.Activity
import android.view.LayoutInflater
import androidx.viewbinding.ViewBinding

inline fun <VB : ViewBinding> Activity.binding(
    crossinline inflate: (LayoutInflater) -> VB
) = lazy {
    inflate(layoutInflater).apply {
        setContentView(root)
    }
}
