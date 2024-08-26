package it.sephiroth.android.rxjava3.extensions.observers

import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.observers.DisposableObserver

class EmitterObserver<T>(private val emitter: ObservableEmitter<T>) :
    DisposableObserver<T>() where T : Any {
    init {
        // Dispose the subscriptions disposable when the emitter is disposed
        emitter.setDisposable(this)
    }

    override fun onNext(t: T) {
        emitter.onNext(t)
    }

    override fun onError(e: Throwable) {
        emitter.tryOnError(e)
    }

    override fun onComplete() {
        emitter.onComplete()
    }
}
