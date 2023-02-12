package ren.imyan.app_tracker.net

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import org.koin.dsl.module

// 超时时间
private const val DEFAULT_TIMEOUT = 15L

// 最大连接数
private const val MAX_LIMIT_CONNECTIONS = 10

val netModule = module {

    factory {
        HttpClient(Android) {
            defaultRequest {
                url {
                    protocol = URLProtocol.HTTP
//                    host = "apptracker-api.cn2.tiers.top/api"
                    host = "apptracker-dev.cn2.tiers.top"
                }

                header("Content-Type", "application/json")
            }

            install(ContentNegotiation) {
                json()
            }

            install(NetLoggingPlugin)
        }
    }

    single {
        AppTrackerRepo()
    }
}