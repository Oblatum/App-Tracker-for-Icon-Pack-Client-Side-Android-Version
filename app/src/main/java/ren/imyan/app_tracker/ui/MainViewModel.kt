package ren.imyan.app_tracker.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.internal.filterList
import ren.imyan.app_tracker.App
import ren.imyan.app_tracker.FilterAppType
import ren.imyan.app_tracker.base.BaseLoad
import ren.imyan.app_tracker.base.BaseViewModel
import ren.imyan.app_tracker.common.ktx.get
import ren.imyan.app_tracker.common.ktx.inject
import ren.imyan.app_tracker.model.AppInfo
import ren.imyan.app_tracker.net.AppTrackerRepo


class MainViewModel : BaseViewModel<MainData, MainEvent, MainAction>() {

    private var currProgress = 0
    private var allAppList: MutableList<AppInfo>? = null
    private var currAppList: MutableList<AppInfo>? = null

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
            is MainAction.FilterApp -> filterList(action.type)
            is MainAction.Search -> search(action.type)
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
                    activityName = activityName(it),
                    icon = it.applicationInfo.loadIcon(get<Context>().packageManager).toBitmap(),
                    isSystem = isSystemApp(it),
                )
            )
        }
        allAppList = appInfoList.filter { it.activityName != "" }.toMutableList()
        currAppList = appInfoList.filter { it.activityName != "" }.toMutableList()
        emitData {
            copy(
                appInfoList = BaseLoad.Success(appInfoList.filter { it.activityName != "" })
            )
        }
    }

    private fun search(type: String) {
        val tempList = currAppList
        tempList?.let { list ->
            var newList = list.filter {
                (it.appName ?: "").contains(type) || (it.packageName ?: "").contains(type)
            }
            if(newList.isEmpty()){
                newList = currAppList!!
            }
            emitData {
                copy(
                    appInfoList = BaseLoad.Success(newList.toMutableList())
                )
            }
        }

    }

    private fun isSystemApp(pi: PackageInfo): Boolean {
        val isSysApp = pi.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 1
        val isSysUpd = pi.applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 1
        return isSysApp || isSysUpd
    }

    private fun activityName(pi: PackageInfo): String {
        val resolveIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(pi.packageName)
        }
        val resolveInfoList = get<Context>().packageManager.queryIntentActivities(resolveIntent, 0)
        kotlin.runCatching {
            val resolveInfo = resolveInfoList.iterator().next()
            resolveInfo?.let {
                return it.activityInfo.name;
            }
        }
        return ""
    }

    private fun filterList(type: FilterAppType) {
        when (type) {
            FilterAppType.User -> {
                allAppList?.let {
                    val newList = it.filter { data -> data.isSystem == false }
                    currAppList = newList.toMutableList()
                    emitData {
                        copy(
                            appInfoList = BaseLoad.Success(newList.toMutableList())
                        )
                    }
                }
            }
            FilterAppType.System -> {
                allAppList?.let {
                    val newList = it.filter { data -> data.isSystem == true }
                    currAppList = newList.toMutableList()
                    emitData {
                        copy(
                            appInfoList = BaseLoad.Success(newList.toMutableList())
                        )
                    }
                }
            }
            FilterAppType.All -> {
                allAppList?.let {
                    currAppList = it.toMutableList()
                    emitData {
                        copy(
                            appInfoList = BaseLoad.Success(it.toMutableList())
                        )
                    }
                }
            }
        }
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
                if (infoList.size == currProgress) {
                    emitEvent {
                        MainEvent.DismissDialog
                    }
                }
            }.launchIn(viewModelScope)
        }.launchIn(viewModelScope)
    }
}