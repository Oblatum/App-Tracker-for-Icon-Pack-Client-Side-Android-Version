package ren.imyan.app_tracker

import android.app.Application
import com.biubiu.eventbus.EventBusInitializer
import com.drake.brv.utils.BRV
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.module.Module
import ren.imyan.app_tracker.net.netModule
import timber.log.Timber

class App:Application() {

    private val moduleList = listOf(netModule)

    override fun onCreate() {
        super.onCreate()
        EventBusInitializer.init(this)
        Timber.plant(Timber.DebugTree())
        // 初始化BindingAdapter的默认绑定ID, 如果不使用DataBinding并不需要初始化
//        BRV.modelId = BR.m
        initKoin()
    }

    private fun initKoin(){
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@App)
            modules(moduleList)
        }
    }
}