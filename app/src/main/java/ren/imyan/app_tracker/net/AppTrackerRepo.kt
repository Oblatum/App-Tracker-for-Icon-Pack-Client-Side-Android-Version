package ren.imyan.app_tracker.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import ren.imyan.app_tracker.common.ktx.get
import ren.imyan.app_tracker.model.AppInfo
import ren.imyan.app_tracker.net.request.SubmitAppRequest
import retrofit2.Retrofit
import java.io.File

class AppTrackerRepo {
    private val api by lazy { get<Retrofit>().create(AppTrackerApi::class.java) }

    suspend fun submitAppInfo(info: AppInfo) =
        flow {
            var signature = "app-tracker"
            if(info.activityName == ""){
                signature = "builtin"
            }
            val appInfo = SubmitAppRequest(
                activityName = info.activityName,
                appName = info.appName,
                packageName = info.packageName,
                signature = signature
            )
            emit(api.submitAppInfo(appInfo))
        }.flowOn(Dispatchers.IO)

    suspend fun submitAppIcon(packageName: String, icon: File) =
        flow {
            val iconFile = icon.asRequestBody("image/jpeg".toMediaTypeOrNull());
            emit(api.submitAppIcon(packageName, iconFile))
        }.flowOn(Dispatchers.IO)
}