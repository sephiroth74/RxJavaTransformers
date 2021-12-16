package it.sephiroth.android.rxjava3.extensions.observers

import io.reactivex.rxjava3.observers.DisposableSingleObserver


/**
 * Auto disposable single observer.
 * Upon completion, or error, will automatically unsubscribe from the
 * upstream
 *
 * @author Alessandro Crugnola on 06.01.21 - 13:25
 */

@Suppress("unused")
class AutoDisposableSingleObserver<T>() : DisposableSingleObserver<T>() {

    private var _doOnSuccess: ((T) -> Unit)? = null
    private var _doOnError: ((Throwable) -> Unit)? = null
    private var _doOnStart: (() -> Unit)? = null

    constructor(builder: (AutoDisposableSingleObserver<T>.() -> Unit)) : this() {
        this.builder()
    }

    override fun onSuccess(t: T) {
        dispose()
        _doOnSuccess?.invoke(t)
    }

    override fun onError(e: Throwable) {
        dispose()
        _doOnError?.invoke(e)
    }

    override fun onStart() {
        super.onStart()
        _doOnStart?.invoke()
    }

    fun doOnSuccess(t: ((T) -> Unit)) {
        _doOnSuccess = t
    }

    fun doOnError(t: ((Throwable) -> Unit)) {
        _doOnError = t
    }

    fun doOnStart(t: (() -> Unit)) {
        _doOnStart = t
    }
}
