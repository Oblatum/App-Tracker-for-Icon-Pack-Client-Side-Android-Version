package ren.imyan.app_tracker.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import ren.imyan.app_tracker.model.AppInfo
import ren.imyan.app_tracker.net.request.SubmitAppRequest
import java.io.File

class AppTrackerRepo {
    private val api = AppTrackerApi

    suspend fun submitAppInfo(info: AppInfo) =
        flow {
            val appInfo = SubmitAppRequest(
                activityName = info.activityName,
                appName = info.appName,
                packageName = info.packageName,
            )
            emit(api.submitAppInfo(appInfo))
        }.flowOn(Dispatchers.IO)

    suspend fun submitAppIcon(packageName: String, icon: File) =
        flow {
            emit(api.submitAppIcon(packageName, icon))
        }.flowOn(Dispatchers.IO)
}