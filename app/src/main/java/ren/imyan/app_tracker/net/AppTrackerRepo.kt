package ren.imyan.app_tracker.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import ren.imyan.app_tracker.common.ktx.get
import ren.imyan.app_tracker.model.AppInfo
import ren.imyan.app_tracker.net.request.SubmitAppRequest
import retrofit2.Retrofit

class AppTrackerRepo {
    private val api by lazy { get<Retrofit>().create(AppTrackerApi::class.java) }

    suspend fun upload(info: AppInfo) =
        flow {
            val appInfo = SubmitAppRequest(
                activityName = info.activityName,
                appName = info.appName,
                packageName = info.packageName,
                signature = "app-tracker"
            )
            emit(api.submitApp(appInfo))
        }.flowOn(Dispatchers.IO)
}