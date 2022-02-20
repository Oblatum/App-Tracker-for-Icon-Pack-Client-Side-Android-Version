package ren.imyan.app_tracker.model

import android.graphics.Bitmap
import androidx.databinding.BaseObservable

data class AppInfo(
    val appName: String?,
    val packageName: String?,
    val activityName: String?,
    val icon: Bitmap?,
    val isSystem: Boolean?,
    var isCheck:Boolean = false
) : BaseObservable()
