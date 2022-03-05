package ren.imyan.app_tracker.net

import okhttp3.MultipartBody
import okhttp3.RequestBody
import ren.imyan.app_tracker.net.request.SubmitAppRequest
import retrofit2.Response
import retrofit2.http.*

interface AppTrackerApi {
    @POST("appInfo")
    suspend fun submitAppInfo(@Body appInfo: SubmitAppRequest): Response<Unit>

    @POST("appIcon")
    suspend fun submitAppIcon(@Query("packageName") packageName:String,@Body icon: RequestBody):Response<Unit>
}