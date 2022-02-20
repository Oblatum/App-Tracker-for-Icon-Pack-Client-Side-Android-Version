package ren.imyan.app_tracker.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ren.imyan.app_tracker.App
import ren.imyan.app_tracker.base.BaseLoad
import ren.imyan.app_tracker.base.BaseViewModel
import ren.imyan.app_tracker.common.ktx.get
import ren.imyan.app_tracker.common.ktx.inject
import ren.imyan.app_tracker.model.AppInfo
import ren.imyan.app_tracker.net.AppTrackerRepo


class MainViewModel : BaseViewModel<MainData, MainEvent, MainAction>() {

    private var currProgress = 0

    init {
        viewModelScope.launch {
            getAppInfo()
        }
    }

    private val repo by inject<AppTrackerRepo>()

    override fun createInitialState(): MainData = MainData()

    override fun dispatch(action: MainAction) {
        when (action) {
            is MainAction.Upload -> {
                currProgress = 0
                action.infoList?.let { sendAppInfoList(it) }
            }
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun getAppInfo() {
        emitData {
            copy(
                appInfoList = BaseLoad.Loading
            )
        }
        val packages = get<Context>().packageManager.getInstalledPackages(0)
        val appInfoList = ArrayList<AppInfo>()
        packages.forEach {
            appInfoList.add(
                AppInfo(
                    appName = it.applicationInfo.loadLabel(get<Context>().packageManager)
                        .toString(),
                    packageName = it.packageName,
                    activityName = it.packageName,
                    icon = it.applicationInfo.loadIcon(get<Context>().packageManager).toBitmap(),
                    isSystem = isSystemApp(it),
                )
            )
        }
        emitData {
            copy(
                appInfoList = BaseLoad.Success(appInfoList)
            )
        }
    }

    private fun isSystemApp(pi: PackageInfo): Boolean {
        val isSysApp = pi.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 1
        val isSysUpd = pi.applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 1
        return isSysApp || isSysUpd
    }

    private fun sendAppInfoList(infoList: List<AppInfo>) {
        infoList.asFlow().onEach {
            repo.upload(it).catch {
                emitEvent {
                    MainEvent.UploadFail
                }
            }.onEach {
                emitEvent {
                    MainEvent.UpdateProgress(currProgress++)
                }
                if(infoList.size == currProgress){
                    emitEvent {
                        MainEvent.DismissDialog
                    }
                }
            }.launchIn(viewModelScope)
        }.launchIn(viewModelScope)
    }
}