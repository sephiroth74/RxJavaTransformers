package it.sephiroth.android.rxjava3.extensions.observers

import io.reactivex.rxjava3.observers.DisposableMaybeObserver

/**
 * Auto disposable Maybe Observer
 * Upon completion, or error, will automatically unsubscribe from the
 * upstream
 *
 * @author Alessandro Crugnola on 06.01.21 - 13:26
 */
@Suppress("unused")
class AutoDisposableMaybeObserver<T>() : DisposableMaybeObserver<T>() {

    private var _doOnSuccess: ((T) -> Unit)? = null
    private var _doOnError: ((Throwable) -> Unit)? = null
    private var _doOnComplete: (() -> Unit)? = null
    private var _doOnFinish: (() -> Unit)? = null
    private var _doOnStart: (() -> Unit)? = null

    constructor(builder: (AutoDisposableMaybeObserver<T>.() -> Unit)) : this() {
        this.builder()
    }

    override fun onComplete() {
        dispose()
        _doOnComplete?.invoke()
        _doOnFinish?.invoke()
    }

    override fun onSuccess(t: T) {
        dispose()
        _doOnSuccess?.invoke(t)
        _doOnFinish?.invoke()
    }

    override fun onError(e: Throwable) {
        dispose()
        _doOnError?.invoke(e)
        _doOnFinish?.invoke()
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

    fun doOnComplete(t: (() -> Unit)) {
        _doOnComplete = t
    }

    fun doOnFinish(t: (() -> Unit)) {
        _doOnFinish = t
    }

    fun doOnStart(t: (() -> Unit)) {
        _doOnStart = t
    }
}
