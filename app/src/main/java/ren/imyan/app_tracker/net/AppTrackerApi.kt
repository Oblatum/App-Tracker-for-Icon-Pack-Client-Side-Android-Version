package ren.imyan.app_tracker.net

import ren.imyan.app_tracker.net.request.SubmitAppRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AppTrackerApi {
    @POST("new")
    suspend fun submitApp(@Body appInfo: SubmitAppRequest): Response<Unit>
}