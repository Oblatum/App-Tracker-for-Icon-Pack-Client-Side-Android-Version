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
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
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
import ren.imyan.app_tracker.databinding.DialogSubmitBinding
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
                                "?????????????????????",
                                "?????? APP ???????????????",
                                "?????? APP ???????????????????????????",
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
????????????${data.appName}
?????????${data.packageName}
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
                    "?????? APP ???????????????????????????",
                    "????????? APP ??????????????????",
                    "????????? APP ??????????????????",
                )
                val submitDialogBinding =
                    DialogSubmitBinding.inflate(this@MainActivity.layoutInflater)

                val selectDialog = MaterialAlertDialogBuilder(this@MainActivity).apply {
                    setView(submitDialogBinding.root)
                    setItems(selectItems) { _, index ->
                        @Suppress("UNCHECKED_CAST")
                        val checkedList =
                            (binding.appList.models as List<AppInfo>).filter { it.isCheck }
                        if (checkedList.isEmpty()) {
                            return@setItems
                        }

                        if (index in 1..2) {
                            dialog.show(supportFragmentManager, "upload")
                            dialog.setTotal(checkedList.size)
                        }

                        when (index) {
                            0 -> {
                                // ?????? APP ???????????????????????????
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
                            1 -> {
                                // ????????? APP ??????????????????
                                viewModel.dispatch(MainAction.SubmitAppInfo(checkedList))
                            }
                            2 -> {
                                // ????????? APP ??????????????????
                                val appIconMap = mutableMapOf<String, Bitmap>()
                                checkedList.forEach {
                                    if (it.packageName != null && it.icon != null) {
                                        appIconMap[it.packageName] = it.icon
                                    }
                                }
                                viewModel.dispatch(MainAction.SubmitAppIcon(appIconMap))
                            }
                        }
                    }
                }.create()

                submitDialogBinding.apply {
                    shareZip.setOnClickListener {
                        @Suppress("UNCHECKED_CAST")
                        val checkedList =
                            (binding.appList.models as List<AppInfo>).filter { it.isCheck }
                        if (checkedList.isEmpty()) {
                            return@setOnClickListener
                        }
                        val edit = EditText(this@MainActivity)
                        val dialog = MaterialAlertDialogBuilder(this@MainActivity).apply {
                            setTitle("???????????????????????????,??????????????????")
                            setView(edit)
                            setPositiveButton("??????") { _, _ ->
                                // ??????????????? ZIP ??????
                                viewModel.dispatch(
                                    MainAction.ShareZip(
                                        checkedList,
                                        edit.text.toString()
                                    )
                                )
                            }
                        }
                        dialog.show()
                        selectDialog.dismiss()
                    }
                    submitAll.setOnClickListener {
                        @Suppress("UNCHECKED_CAST")
                        val checkedList =
                            (binding.appList.models as List<AppInfo>).filter { it.isCheck && it.activityName != "" }
                        if (checkedList.isEmpty()) {
                            return@setOnClickListener
                        }
                        dialog.show(supportFragmentManager, "upload")
                        dialog.setTotal(checkedList.size)
                        // ?????????????????????
                        dialog.showTitle()
                        val appIconMap = mutableMapOf<String, Bitmap>()
                        checkedList.forEach {
                            if (it.packageName != null && it.icon != null) {
                                appIconMap[it.packageName] = it.icon
                            }
                        }
                        viewModel.dispatch(MainAction.SubmitAll(checkedList, appIconMap))
                        selectDialog.dismiss()
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
                    Toast.makeText(this@MainActivity, "????????????", Toast.LENGTH_SHORT).show()
                }
                MainEvent.UploadFail -> {
                    dialog.dismiss()
                    Toast.makeText(this@MainActivity, "????????????", Toast.LENGTH_SHORT).show()
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
            R.id.none_activity -> viewModel.dispatch(MainAction.SwitchToShowNoneActivityNameApp)
        }
        return super.onOptionsItemSelected(item)
    }
}