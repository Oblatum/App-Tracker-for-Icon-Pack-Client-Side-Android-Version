package ren.imyan.app_tracker.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * ViewModel -------(Data「UI 数据」)--------> View
 */
interface UiData

/**
 * ViewModel -------(Event「一次性事件」)--------> View
 */
interface UiEvent

/**
 * View -------(Action「指令」)--------> ViewModel
 */
interface UiAction

abstract class BaseViewModel<Data : UiData, Event : UiEvent, Action : UiAction> : ViewModel() {
    /**
     * 初始状态
     */
    private val initialData: Data by lazy { createInitialState() }

    abstract fun createInitialState(): Data

    abstract fun dispatch(action: Action)

    @Suppress("PropertyName")
    protected val _uiData: MutableStateFlow<Data> = MutableStateFlow(initialData)
    val uiData = _uiData.asStateFlow()

    private val _event: Channel<Event> = Channel()
    val uiEvent = _event.receiveAsFlow()

    protected fun emitEvent(builder: () -> Event) {
        val newEvent = builder()
        viewModelScope.launch {
            _event.send(newEvent)
        }
    }

    protected fun emitData(builder: Data.() -> Data) {
        _uiData.value = _uiData.value.builder()
    }
}