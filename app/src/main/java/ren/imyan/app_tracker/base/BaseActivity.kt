package ren.imyan.app_tracker.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import timber.log.Timber

interface UiEventImp<Event> {
    fun renderUiEvent(event: Event)
}

open class BaseActivity : AppCompatActivity() {

    private var firstResumed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.v("当前 Activity ${javaClass.name}")
        initView()
        initViewModel()
    }

    override fun onResume() {
        super.onResume()
        if (!firstResumed) {
            firstResumed = true
            loadSingleData()
        }
        loadData()
    }

    protected open fun initView() {}

    protected open fun initViewModel() {}

    protected open fun loadData() {}

    protected open fun loadSingleData() {}
}