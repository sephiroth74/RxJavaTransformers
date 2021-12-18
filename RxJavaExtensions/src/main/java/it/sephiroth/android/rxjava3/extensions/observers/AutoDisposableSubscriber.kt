package it.sephiroth.android.rxjava3.extensions.observers

import io.reactivex.rxjava3.subscribers.DisposableSubscriber


/**
 * Auto Disposable Observer.
 * Upon completion, or error, will automatically unsubscribe from the
 * upstream
 *
 * @author Alessandro Crugnola on 06.01.21 - 13:21
 */
@Suppress("unused")
open class AutoDisposableSubscriber<T>() : DisposableSubscriber<T>() {

    private var _doOnNext: ((T) -> Unit)? = null
    private var _doOnStart: (() -> Unit)? = null
    private var _doOnComplete: (() -> Unit)? = null
    private var _doOnError: ((Throwable) -> Unit)? = null

    @Suppress("unused")
    constructor(builder: (AutoDisposableSubscriber<T>.() -> Unit)) : this() {
        this.builder()
    }

    override fun onNext(t: T) {
        _doOnNext?.invoke(t)
    }

    override fun onComplete() {
        dispose()
        _doOnComplete?.invoke()
    }

    override fun onStart() {
        super.onStart()
        _doOnStart?.invoke()
    }

    override fun onError(e: Throwable) {
        dispose()
        _doOnError?.invoke(e)
    }

    fun doOnStart(t: (() -> Unit)) {
        _doOnStart = t
    }

    fun doOnNext(t: ((T) -> Unit)) {
        _doOnNext = t
    }

    fun doOnComplete(t: (() -> Unit)) {
        _doOnComplete = t
    }

    fun doOnError(t: ((Throwable) -> Unit)) {
        _doOnError = t
    }
}
