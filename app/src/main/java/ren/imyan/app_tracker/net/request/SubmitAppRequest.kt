package ren.imyan.app_tracker.net.request
import androidx.annotation.Keep

import com.squareup.moshi.JsonClass

import com.squareup.moshi.Json


@Keep
@JsonClass(generateAdapter = true)
data class SubmitAppRequest(
    @Json(name = "activityName")
    val activityName: String?,
    @Json(name = "appName")
    val appName: String?,
    @Json(name = "packageName")
    val packageName: String?,
    @Json(name = "signature")
    val signature: String?
)