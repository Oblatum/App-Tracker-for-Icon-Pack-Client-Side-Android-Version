package ren.imyan.app_tracker.net

import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

// 超时时间
private const val DEFAULT_TIMEOUT = 15L

// 最大连接数
private const val MAX_LIMIT_CONNECTIONS = 10

val netModule = module {
    single {
        Retrofit.Builder()
            .client(get())
            .baseUrl("https://test.k2t3k.tk/api/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    single {
        OkHttpClient.Builder()
            .addInterceptor(LogInterceptor())
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .connectionPool(
                ConnectionPool(
                    MAX_LIMIT_CONNECTIONS,
                    DEFAULT_TIMEOUT,
                    TimeUnit.SECONDS
                )
            )
            .build()
    }

    single {
        AppTrackerRepo()
    }
}