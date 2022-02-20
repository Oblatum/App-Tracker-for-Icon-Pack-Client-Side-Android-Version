package ren.imyan.app_tracker.ui

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
    object DismissDialog : MainEvent()
    object UploadFail : MainEvent()
}

sealed class MainAction : UiAction {
    data class Upload(val infoList: List<AppInfo>? = null) : MainAction()
    data class FilterApp(val type: FilterAppType) : MainAction()
    data class Search(val type: String) : MainAction()
}