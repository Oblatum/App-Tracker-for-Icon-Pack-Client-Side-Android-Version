package ren.imyan.app_tracker.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.drake.brv.utils.models
import com.drake.brv.utils.mutable
import com.drake.brv.utils.setup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zackratos.ultimatebarx.ultimatebarx.navigationBar
import com.zackratos.ultimatebarx.ultimatebarx.statusBar
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ren.imyan.app_tracker.R
import ren.imyan.app_tracker.base.BaseActivity
import ren.imyan.app_tracker.base.BaseLoad
import ren.imyan.app_tracker.common.ktx.binding
import ren.imyan.app_tracker.common.ktx.mapInPlace
import ren.imyan.app_tracker.common.ktx.observeState
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
                        appName.text = data.appName
                        appIcon.load(data.icon)

                        check.isChecked = data.isCheck

                        rootLayout.setOnClickListener {
                            data.isCheck = !data.isCheck
                            notifyItemChanged(adapterPosition)
                        }
                        rootLayout.setOnLongClickListener {
                            MaterialAlertDialogBuilder(this@MainActivity).setMessage(
                                """
                                App Name: ${data.appName}
                                
                                Package Name: ${data.packageName}
                                
                                Activity Name: ${data.activityName}
                            """.trimIndent()
                            ).show()
                            true
                        }
                    }
                }
            }
            send.setOnClickListener {
                val checkedList = (appList.models as List<AppInfo>).filter { it.isCheck }
                if (checkedList.isEmpty()) {
                    return@setOnClickListener
                }

                dialog.show(supportFragmentManager, "upload")
                dialog.setTotal(checkedList.size)

                viewModel.dispatch(MainAction.Upload(checkedList))
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
                    }
                }.launchIn(this@MainActivity.lifecycleScope)
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
                    binding.appList.models =
                        it.data.toMutableList().filter { data -> data.isSystem == false }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar, menu)
        return true
    }

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
        }
        return super.onOptionsItemSelected(item)
    }
}