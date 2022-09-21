package ren.imyan.app_tracker.ui

import ando.file.core.*
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDateTime
import ren.imyan.app_tracker.FilterAppType
import ren.imyan.app_tracker.base.BaseLoad
import ren.imyan.app_tracker.base.BaseViewModel
import ren.imyan.app_tracker.common.ktx.*
import ren.imyan.app_tracker.common.utils.ZipUtil
import ren.imyan.app_tracker.model.AppInfo
import ren.imyan.app_tracker.net.AppTrackerRepo
import java.io.File
import java.io.OutputStream


class MainViewModel : BaseViewModel<MainData, MainEvent, MainAction>() {

    private var currProgress = 0
    private var allAppList: MutableList<AppInfo>? = null
    private var currAppList: MutableList<AppInfo>? = null
    private var showNoneActivityNameApp = false

    init {
        viewModelScope.launch(Dispatchers.IO) {
            getAppInfo()
        }
    }

    private val repo by inject<AppTrackerRepo>()

    override fun createInitialState(): MainData = MainData()

    override fun dispatch(action: MainAction) {
        when (action) {
            is MainAction.SubmitAppInfo -> {
                currProgress = 0
                action.infoList?.let { submitAppInfo(it) }
            }
            is MainAction.FilterApp -> filterList(action.type)
            is MainAction.Search -> search(action.type)
            is MainAction.SubmitAppIcon -> {
                currProgress = 0
                action.iconMap?.let { submitAppIcon(it) }
            }
            is MainAction.SubmitAll -> {
                currProgress = 0
                if (action.infoList != null && action.iconMap != null) {
                    submitAll(action.infoList, action.iconMap)
                }
            }
            is MainAction.SaveIcon -> saveIcon(action.icon, action.appName)
            is MainAction.ShareZip -> action.infoList?.let { shareZip(it, action.iconPackName) }
            MainAction.SwitchToShowNoneActivityNameApp -> {
                showNoneActivityNameApp = !showNoneActivityNameApp
                filterActivityName()
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
                    activityName = activityName(it),
                    icon = it.getOriginalIcon(),
                    isSystem = isSystemApp(it),
                )
            )
        }
        appInfoList.sortBy {
            it.appName
        }
        allAppList = appInfoList.toMutableList()
        currAppList = appInfoList.toMutableList()
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
                (it.appName ?: "").contains(type, ignoreCase = true) || (it.packageName
                    ?: "").contains(type, ignoreCase = true)
            }
            if (type.isEmpty()) {
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
                return it.activityInfo.name
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
                }
            }
            FilterAppType.System -> {
                allAppList?.let {
                    val newList = it.filter { data -> data.isSystem == true }
                    currAppList = newList.toMutableList()
                }
            }
            FilterAppType.All -> {
                allAppList?.let {
                    currAppList = it.toMutableList()
                }
            }
        }
        filterActivityName()
    }

    private fun filterActivityName() {
        currAppList?.let {
            val newList = it.filter { data -> (data.activityName == "") == showNoneActivityNameApp }
            emitData {
                copy(
                    appInfoList = BaseLoad.Success(newList.toMutableList())
                )
            }
        }
    }

    private fun submitAppInfo(infoList: List<AppInfo>) {
        infoList.asFlow().onEach {
            delay(200)
            repo.submitAppInfo(it).catch {
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

    private fun submitAppIcon(iconList: Map<String, Bitmap>) {
        iconList.toList().asFlow().catch { err ->
            err.printStackTrace()
        }.onEach {
            val iconFile = it.second.setBackground().toSize(288f, 288f)
                .toFile("${it.first}.png", format = Bitmap.CompressFormat.PNG)
            if (iconFile != null) {
                delay(200)
                repo.submitAppIcon(it.first, iconFile).catch { err ->
                    err.printStackTrace()
                    emitEvent {
                        MainEvent.UploadFail
                    }
                }.onEach {
                    emitEvent {
                        MainEvent.UpdateProgress(currProgress++)
                    }
                    if (iconList.size == currProgress) {
                        emitEvent {
                            MainEvent.DismissDialog
                        }
                    }
                }.launchIn(viewModelScope)
            }
        }.launchIn(viewModelScope)
    }

    private fun submitAll(infoList: List<AppInfo>, iconList: Map<String, Bitmap>) {
        infoList.asFlow().onEach {
            delay(200)
            repo.submitAppInfo(it).catch {
                emitEvent {
                    MainEvent.UploadFail
                }
            }.onEach {
                emitEvent {
                    MainEvent.UpdateProgress(currProgress++)
                }
                if (infoList.size == currProgress) {
                    currProgress = 0
                    MainEvent.UpdateProgress(0)
                    emitEvent {
                        MainEvent.SwitchTitle
                    }
                    iconList.toList().asFlow().catch { err ->
                        err.printStackTrace()
                    }.onEach { icons ->
                        val iconFile = icons.second.setBackground().toSize(288f, 288f)
                            .toFile("${icons.first}.png", format = Bitmap.CompressFormat.PNG)
                        if (iconFile != null) {
                            delay(200)
                            repo.submitAppIcon(icons.first, iconFile).catch { err ->
                                err.printStackTrace()
                                emitEvent {
                                    MainEvent.UploadFail
                                }
                            }.onEach {
                                emitEvent {
                                    MainEvent.UpdateProgress(currProgress++)
                                }
                                if (iconList.size == currProgress) {
                                    emitEvent {
                                        MainEvent.DismissDialog
                                    }
                                }
                            }.launchIn(viewModelScope)
                        }
                    }.launchIn(viewModelScope)
                }
            }.launchIn(viewModelScope)
        }.launchIn(viewModelScope)
    }

    private fun saveIcon(icon: Bitmap?, appName: String?) {
        viewModelScope.launch {
            flow {
                emit(icon?.toFile("${appName}.png", format = Bitmap.CompressFormat.PNG))
            }.flowOn(Dispatchers.IO).collect {
                val contentValue = MediaStoreUtils.createContentValues(
                    displayName = "${appName}.png",
                    mimeType = "image/png",
                    relativePath = "${Environment.DIRECTORY_PICTURES}/app-track"
                )
                insertBitmap(
                    BitmapFactory.decodeFile(it?.path),
                    contentValue
                )
                Toast.makeText(get(), "保存图标成功", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun insertBitmap(bitmap: Bitmap?, values: ContentValues): Uri? {
        val externalUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val resolver = FileOperator.getContext().contentResolver
        val insertUri = resolver.insert(externalUri, values)
        //标记当前文件是 Pending 状态
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.IS_PENDING, 1)
            //MediaStore.setIncludePending(insertUri)
        }
        var os: OutputStream? = null
        try {
            if (insertUri != null && bitmap != null) {
                os = resolver.openOutputStream(insertUri)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                os?.flush()

                FileLogger.d("创建Bitmap成功 insertBitmap $insertUri")

                //https://developer.android.google.cn/training/data-storage/files/media#native-code
                // Now that we're finished, release the "pending" status, and allow other apps to view the image.
                values.clear()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(insertUri, values, null, null)
                }
            }
        } catch (e: Exception) {
            FileLogger.d("创建失败：${e.message}")
        } finally {
            if (bitmap?.isRecycled == false) bitmap.recycle()
            try {
                os?.close()
            } catch (t: Throwable) {
            }
        }
        return insertUri
    }

    private fun shareZip(infoList: List<AppInfo>, iconPackName: String) {
        val time = LocalDateTime.now()
        infoList.asFlow().flowOn(Dispatchers.IO).onStart {
            val stringBuilder = StringBuilder().apply {
                append("<resources>")
                infoList.forEach {
                    append("\n")
                    append("<!-- ${it.appName} -->\n")
                    append("<item component=\"ComponentInfo{${it.packageName}/${it.activityName}}\" drawable=\"${it.appName}\"/>\n")
                    append("\n")
                }
                append("</resources>")
            }
            val appFilterFile = FileUtils.createFile(
                "${get<Context>().cacheDir.path}/${time}",
                "appfilter.xml"
            )
            if (appFilterFile != null) {
                FileUtils.writeBytes2File(
                    stringBuilder.toString().toByteArray(), appFilterFile
                )
            }
        }.onEach {
            it.icon?.toFile(
                "${it.appName!!}.png",
                "${get<Context>().cacheDir.path}/${time}",
                Bitmap.CompressFormat.PNG
            )
        }.onCompletion {
            ZipUtil.zipByFolder(
                "${get<Context>().cacheDir.path}/${time}",
                "${get<Context>().cacheDir.path}/${iconPackName}_ICON_${time}.zip"
            )

            val uri = FileUri.getUriByFile(File("${get<Context>().cacheDir.path}/${iconPackName}_ICON_${time}.zip"))

            uri?.let {
                FileOpener.openShare(get(), uri, "${iconPackName}_ICON_${time}.zip")
            }
        }.launchIn(viewModelScope)
    }
}
