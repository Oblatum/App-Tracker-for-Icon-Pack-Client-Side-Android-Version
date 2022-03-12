package ren.imyan.app_tracker.ui

import ando.file.core.FileOpener
import ando.file.core.FileOperator
import ando.file.core.FileUtils
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.core.view.updatePadding
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import coil.load
import com.drake.brv.utils.models
import com.drake.brv.utils.mutable
import com.drake.brv.utils.setup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zackratos.ultimatebarx.ultimatebarx.navigationBar
import com.zackratos.ultimatebarx.ultimatebarx.navigationBarHeight
import com.zackratos.ultimatebarx.ultimatebarx.statusBar
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ren.imyan.app_tracker.FilterAppType
import ren.imyan.app_tracker.R
import ren.imyan.app_tracker.base.BaseActivity
import ren.imyan.app_tracker.base.BaseLoad
import ren.imyan.app_tracker.common.ktx.*
import ren.imyan.app_tracker.databinding.ActivityMainBinding
import ren.imyan.app_tracker.databinding.ItemAppBinding
import ren.imyan.app_tracker.model.AppInfo


class MainActivity : BaseActivity() {


    private val binding by binding(ActivityMainBinding::inflate)
    private val viewModel by viewModels<MainViewModel>()
    private val dialog by lazy {
        UploadDialog().apply {
            isCancelable = false
        }
    }

    override fun initView() {
        super.initView()
        FileUtils.deleteFile(get<Context>().cacheDir.path)

        statusBar {
            color = Color.WHITE
            light = true
        }
        navigationBar {
            transparent()
        }
        binding.apply {
            setSupportActionBar(toolbar)

            appList.setup {
                addType<AppInfo>(R.layout.item_app)
                onBind {
                    val binding = getBinding<ItemAppBinding>()
                    val data = mutable[modelPosition] as AppInfo

                    binding.apply {
                        if (modelPosition == modelCount) {
                            rootLayout.updatePadding(bottom = navigationBarHeight)
                        }

                        appName.text = data.appName
                        appIcon.load(data.icon)

                        check.isChecked = data.isCheck

                        rootLayout.setOnClickListener {
                            data.isCheck = !data.isCheck
                            notifyItemChanged(adapterPosition)
                        }
                        rootLayout.setOnLongClickListener {
                            val selectItems = arrayOf(
                                "保存图标到相册",
                                "复制 APP 名称和包名",
                                "复制 APP 名称、包名和启动项",
                            )
                            MaterialAlertDialogBuilder(this@MainActivity).apply {
                                setItems(selectItems) { _, index ->
                                    when (index) {
                                        0 -> {
                                            viewModel.dispatch(
                                                MainAction.SaveIcon(
                                                    data.icon,
                                                    data.appName
                                                )
                                            )
                                        }
                                        1 -> {
                                            """
应用名：${data.appName}
包名：${data.packageName}
                                           """.trimIndent().copy()
                                        }
                                        2 -> {
                                            """
<item component="ComponentInfo{${data.packageName}/${data.activityName}}" drawable="${data.appName}"/>
                                            """.trimIndent().copy()
                                        }
                                    }
                                }
                            }.show()
                            true
                        }
                    }
                }
            }
            searchEdit.doOnTextChanged { text, _, _, _ ->
                viewModel.dispatch(MainAction.Search(text.toString().trim()))
            }

            send.setOnClickListener {
                val selectItems = arrayOf(
                    "分享 ZIP 文件（推荐使用！）",
                    "复制 APP 名称和包名到剪切板",
                    "只上传 APP 信息到服务器",
                    "只上传 APP 图标到服务器",
                    "都上传到服务器",
                )

                val selectDialog = MaterialAlertDialogBuilder(this@MainActivity).apply {
                    setItems(selectItems) { _, index ->
                        @Suppress("UNCHECKED_CAST")
                        val checkedList =
                            (binding.appList.models as List<AppInfo>).filter { it.isCheck }
                        if (checkedList.isEmpty()) {
                            return@setItems
                        }

                        if (index in 2..4) {
                            dialog.show(supportFragmentManager, "upload")
                            dialog.setTotal(checkedList.size)
                        }

                        when (index) {
                            0 -> {
                                // 导出信息为 ZIP 文件
                                viewModel.dispatch(MainAction.ShareZip(checkedList))
                            }
                            1 -> {
                                // 复制 APP 名称和包名到剪切板
                                val stringBuilder = StringBuilder().apply {
                                    append("<resources>")
                                    checkedList.forEach {
                                        append("\n")
                                        append("<!-- ${it.appName} -->\n")
                                        append("<item component=\"ComponentInfo{${it.packageName}/${it.activityName}}\" drawable=\"${it.appName}\"/>\n")
                                        append("\n")
                                    }
                                    append("</resources>")
                                }
                                stringBuilder.toString().copy()
                            }
                            2 -> {
                                // 只上传 APP 信息到服务器
                                viewModel.dispatch(MainAction.SubmitAppInfo(checkedList))
                            }
                            3 -> {
                                // 只上传 APP 图标到服务器
                                val appIconMap = mutableMapOf<String, Bitmap>()
                                checkedList.forEach {
                                    if (it.packageName != null && it.icon != null) {
                                        appIconMap[it.packageName] = it.icon
                                    }
                                }
                                viewModel.dispatch(MainAction.SubmitAppIcon(appIconMap))
                            }
                            4 -> {
                                // 都上传到服务器
                                dialog.showTitle()
                                val appIconMap = mutableMapOf<String, Bitmap>()
                                checkedList.forEach {
                                    if (it.packageName != null && it.icon != null) {
                                        appIconMap[it.packageName] = it.icon
                                    }
                                }
                                viewModel.dispatch(MainAction.SubmitAll(checkedList, appIconMap))
                            }
                        }
                    }
                }
                selectDialog.show()
            }
        }
    }


    override fun initViewModel() {
        super.initViewModel()
        viewModel.uiData.observeState(this, MainData::appInfoList) {
            when (it) {
                is BaseLoad.Error -> {

                }
                BaseLoad.Loading -> {

                }
                is BaseLoad.Success -> {
                    binding.progress.isGone = true
                    binding.appList.models =
                        it.data.toMutableList()
                }
                else -> {}
            }
        }
        viewModel.uiEvent.onEach {
            when (it) {
                is MainEvent.UpdateProgress -> dialog.updateProgress(it.progress)
                MainEvent.DismissDialog -> {
                    dialog.dismiss()
                    Toast.makeText(this@MainActivity, "上传完成", Toast.LENGTH_SHORT).show()
                }
                MainEvent.UploadFail -> {
                    dialog.dismiss()
                    Toast.makeText(this@MainActivity, "上传失败", Toast.LENGTH_SHORT).show()
                }
                MainEvent.SwitchTitle -> dialog.switchTitle()
            }
        }.launchIn(this@MainActivity.lifecycleScope)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar, menu)
        return true
    }

    @Suppress("UNCHECKED_CAST")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.select_all -> {
                val oldList = binding.appList.mutable as MutableList<AppInfo>
                oldList.mapInPlace {
                    it.isCheck = true
                    it
                }
                binding.appList.models = oldList
            }
            R.id.ic_select_un_all -> {
                val oldList = binding.appList.mutable as MutableList<AppInfo>
                oldList.mapInPlace {
                    it.isCheck = false
                    it
                }
                binding.appList.models = oldList
            }
            R.id.ic_select_revers -> {
                val oldList = binding.appList.mutable as MutableList<AppInfo>
                oldList.mapInPlace {
                    if (it.isCheck) {
                        it.isCheck = false
                        it
                    } else {
                        it.isCheck = true
                        it
                    }
                }
                binding.appList.models = oldList
            }
            R.id.only_user_app -> viewModel.dispatch(MainAction.FilterApp(FilterAppType.User))
            R.id.only_system_app -> viewModel.dispatch(MainAction.FilterApp(FilterAppType.System))
            R.id.all_app -> viewModel.dispatch(MainAction.FilterApp(FilterAppType.All))
            R.id.about -> startActivity(Intent(this, AboutActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }
}