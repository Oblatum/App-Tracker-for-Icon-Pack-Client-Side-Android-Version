package ren.imyan.app_tracker.ui

import android.graphics.Bitmap
import ren.imyan.app_tracker.FilterAppType
import ren.imyan.app_tracker.base.BaseLoad
import ren.imyan.app_tracker.base.UiAction
import ren.imyan.app_tracker.base.UiData
import ren.imyan.app_tracker.base.UiEvent
import ren.imyan.app_tracker.model.AppInfo

data class MainData(
    val appInfoList: BaseLoad<List<AppInfo>>? = null
) : UiData

sealed class MainEvent : UiEvent {
    data class UpdateProgress(val progress: Int) : MainEvent()
    object SwitchTitle : MainEvent()
    object DismissDialog : MainEvent()
    object UploadFail : MainEvent()
}

sealed class MainAction : UiAction {
    data class SubmitAppInfo(val infoList: List<AppInfo>? = null) : MainAction()
    data class SubmitAppIcon(val iconMap: Map<String, Bitmap>? = null) : MainAction()
    data class SubmitAll(
        val infoList: List<AppInfo>? = null,
        val iconMap: Map<String, Bitmap>? = null
    ) : MainAction()

    data class FilterApp(val type: FilterAppType) : MainAction()
    data class Search(val type: String) : MainAction()
}