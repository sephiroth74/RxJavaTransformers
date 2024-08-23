package it.sephiroth.android.rxjava3.extensions.observers

import androidx.annotation.CallSuper
import io.reactivex.rxjava3.core.SingleObserver
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.internal.disposables.DisposableHelper
import io.reactivex.rxjava3.internal.util.EndConsumerHelper
import java.util.concurrent.atomic.AtomicReference

abstract class DisposableSingleObserver<T> : SingleObserver<T>, Disposable where T : Any {
    val upstream: AtomicReference<Disposable> = AtomicReference()

    override fun onSubscribe(d: Disposable) {
        if (EndConsumerHelper.setOnce(this.upstream, d, javaClass)) {
            onStart()
        }
    }

    /**
     * Called once the single upstream [Disposable] is set via [.onSubscribe].
     */
    protected abstract fun onStart()

    protected abstract fun onDispose()

    override fun isDisposed(): Boolean {
        return upstream.get() === DisposableHelper.DISPOSED
    }

    @CallSuper
    override fun dispose() {
        if (DisposableHelper.dispose(upstream)) {
            onDispose()
        }
    }
}
