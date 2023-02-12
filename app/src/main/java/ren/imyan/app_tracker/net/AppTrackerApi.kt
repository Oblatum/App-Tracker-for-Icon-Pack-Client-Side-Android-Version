package ren.imyan.app_tracker.net

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.util.cio.*
import ren.imyan.app_tracker.common.ktx.get
import ren.imyan.app_tracker.net.request.SubmitAppRequest
import java.io.File

object AppTrackerApi {
    private val client = get<HttpClient>()
    suspend fun submitAppInfo(appInfo: SubmitAppRequest) = client.post("/api/appinfo") {
        setBody(appInfo)
    }

    suspend fun submitAppIcon(packageName: String, icon: File) =
        client.post("/api/icon") {
            headers {
                remove("Content-Type")
                append("Content-Type", "image/png")
            }
            url {
                parameter("packageName", packageName)
            }
            setBody(icon.readChannel())
        }
}