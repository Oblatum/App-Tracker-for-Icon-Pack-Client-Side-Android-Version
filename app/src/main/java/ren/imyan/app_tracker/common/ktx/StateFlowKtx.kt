package ren.imyan.app_tracker.common.ktx

import androidx.lifecycle.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty1

fun <T, A> StateFlow<T>.observeState(
    lifecycleOwner: LifecycleOwner,
    prop1: KProperty1<T, A>,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    action: (A) -> Unit
) {
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.lifecycle.repeatOnLifecycle(state) {
            this@observeState.map {
                StateTuple11(prop1.get(it))
            }.distinctUntilChanged().collect { (a) ->
                action.invoke(a)
            }
        }
    }
}

fun <T, A, B> StateFlow<T>.observeState(
    lifecycleOwner: LifecycleOwner,
    prop1: KProperty1<T, A>,
    prop2: KProperty1<T, B>,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    action: (A, B) -> Unit
) {
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.lifecycle.repeatOnLifecycle(state) {
            this@observeState.map {
                StateTuple22(prop1.get(it), prop2.get(it))
            }.distinctUntilChanged().collect { (a, b) ->
                action.invoke(a, b)
            }
        }
    }
}

internal data class StateTuple11<A>(val a: A)
internal data class StateTuple22<A, B>(val a: A, val b: B)

fun <T> MutableStateFlow<T>.setState(block: T.() -> T) {
    this.value = this.value.block()
}