package ren.imyan.app_tracker.net.request
import androidx.annotation.Keep

import kotlinx.serialization.SerialName


@Keep
@kotlinx.serialization.Serializable
data class SubmitAppRequest(
    @SerialName("activityName")
    val activityName: String?,
    @SerialName("appName")
    val appName: String?,
    @SerialName("packageName")
    val packageName: String?,
)