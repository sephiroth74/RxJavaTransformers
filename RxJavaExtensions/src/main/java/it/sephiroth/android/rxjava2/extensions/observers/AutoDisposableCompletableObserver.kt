package it.sephiroth.android.rxjava2.extensions.observers

import io.reactivex.observers.DisposableCompletableObserver


/**
 * Auto disposable Completable Observer
 * Upon completion, or error, will automatically unsubscribe from the
 * upstream
 *
 * @author Alessandro Crugnola on 06.01.21 - 13:23
 */
@Suppress("unused")
class AutoDisposableCompletableObserver() : DisposableCompletableObserver() {

    private var _doOnComplete: (() -> Unit)? = null
    private var _doOnError: ((Throwable) -> Unit)? = null
    private var _doOnStart: (() -> Unit)? = null

    constructor(builder: (AutoDisposableCompletableObserver.() -> Unit)) : this() {
        this.builder()
    }

    override fun onComplete() {
        dispose()
        _doOnComplete?.invoke()
    }

    override fun onError(e: Throwable) {
        dispose()
        _doOnError?.invoke(e)
    }

    override fun onStart() {
        super.onStart()
        _doOnStart?.invoke()
    }

    fun doOnComplete(t: (() -> Unit)) {
        _doOnComplete = t
    }

    fun doOnError(t: ((Throwable) -> Unit)) {
        _doOnError = t
    }

    fun doOnStart(t: (() -> Unit)) {
        _doOnStart = t
    }
}
