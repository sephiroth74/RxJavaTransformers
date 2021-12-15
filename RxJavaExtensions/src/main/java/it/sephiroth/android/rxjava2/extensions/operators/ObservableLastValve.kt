@file:Suppress("unused")

package it.sephiroth.android.rxjava2.extensions.operators


import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.internal.disposables.DisposableHelper
import io.reactivex.internal.util.AtomicThrowable
import io.reactivex.plugins.RxJavaPlugins
import java.io.Serializable
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Allows stopping and resuming the flow of the main source when a secondary flow
 * signals false and true respectively.
 * When resumed only the last emitted value (if available) of the source stream will be emitted.
 *
 *
 * @param <T> the main source's value type
 * @param other the observable which will open/close the valve
 * @param defaultOpen true if the valve if to be considered opened by default
 */
class ObservableLastValve<T>(
    private val other: ObservableSource<Boolean>,
    private val defaultOpen: Boolean
) : Observable<T>(), ObservableTransformer<T, T> {
    private lateinit var source: Observable<out T>

    constructor(
        source: Observable<out T>,
        other: ObservableSource<Boolean>,
        defaultOpen: Boolean
    ) : this(other, defaultOpen) {
        this.source = source
    }

    override fun subscribeActual(observer: Observer<in T>) {
        val parent = ValveMainObserver(observer, defaultOpen)
        observer.onSubscribe(parent)
        other.subscribe(parent.other)
        source.subscribe(parent)
    }

    override fun apply(upstream: Observable<T>): Observable<T> {
        return ObservableLastValve(upstream, other, defaultOpen)
    }

    internal class ValveMainObserver<T>(
        private val downstream: Observer<in T>,
        defaultOpen: Boolean
    ) : Observer<T>, Disposable, Serializable {
        private val upstream: AtomicReference<Disposable> = AtomicReference()
        internal val other: OtherSubscriber = OtherSubscriber()
        private val error: AtomicThrowable = AtomicThrowable()
        private val queue: AtomicReference<T> = AtomicReference()
        private val semaphore = AtomicInteger()

        @Volatile
        var done = false

        @Volatile
        var gate: Boolean = defaultOpen

        @Volatile
        var cancelled = false

        override fun onSubscribe(d: Disposable) {
            DisposableHelper.setOnce(upstream, d)
        }

        override fun onNext(t: T) {
            queue.set(t)
            drain()
        }

        override fun onError(t: Throwable) {
            if (error.addThrowable(t)) {
                drain()
            } else {
                RxJavaPlugins.onError(t)
            }
        }

        override fun onComplete() {
            done = true
            drain()
        }

        override fun isDisposed(): Boolean {
            return cancelled
        }

        override fun dispose() {
            cancelled = true
            DisposableHelper.dispose(upstream)
            DisposableHelper.dispose(other)
        }

        private fun drain() {
            val value = semaphore.getAndIncrement()
            if (value != 0) {
                return
            }
            var missed = 1
            val q = queue
            val a = downstream
            val error = error
            do {
                while (true) {
                    if (cancelled) {
                        q.set(null)
                        return
                    }
                    if (error.get() != null) {
                        val ex = error.terminate()
                        q.set(null)
                        DisposableHelper.dispose(upstream)
                        DisposableHelper.dispose(other)
                        a.onError(ex)
                        return
                    }
                    if (!gate) {
                        break
                    }
                    val d = done
                    val v = q.getAndSet(null)
                    val empty = v == null
                    if (d && empty) {
                        DisposableHelper.dispose(other)
                        a.onComplete()
                        return
                    }
                    if (empty) {
                        break
                    }
                    a.onNext(v)
                }
                missed = semaphore.addAndGet(-missed)
            } while (missed != 0)
        }

        fun change(state: Boolean) {
            gate = state
            if (state) {
                drain()
            }
        }

        fun innerError(ex: Throwable) {
            onError(ex)
        }

        fun innerComplete() {
            innerError(IllegalStateException("The valve source completed unexpectedly."))
        }

        internal inner class OtherSubscriber : AtomicReference<Disposable?>(), Observer<Boolean>,
            Serializable {
            override fun onSubscribe(d: Disposable) {
                DisposableHelper.setOnce(this, d)
            }

            override fun onNext(t: Boolean) {
                change(t)
            }

            override fun onError(t: Throwable) {
                innerError(t)
            }

            override fun onComplete() {
                innerComplete()
            }
        }
    }
}

