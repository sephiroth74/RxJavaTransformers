/*
 * MIT License
 *
 * Copyright (c) 2021 Alessandro Crugnola
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

@file:Suppress("unused")

package it.sephiroth.android.rxjava3.extensions.operators

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableSource
import io.reactivex.rxjava3.core.ObservableTransformer
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.internal.disposables.DisposableHelper
import io.reactivex.rxjava3.internal.util.AtomicThrowable
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
) : Observable<T>(), ObservableTransformer<T, T> where T : Any {
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
    ) : Observer<T>, Disposable, Serializable where T : Any {
        private val queue: AtomicReference<T> = AtomicReference()
        internal val other: OtherSubscriber = OtherSubscriber()
        private val error: AtomicThrowable = AtomicThrowable()
        private val upstream: AtomicReference<Disposable> = AtomicReference()
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
            if (error.tryAddThrowableOrReport(t)) {
                drain()
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
            error.tryTerminateAndReport()
        }

        private fun drain() {
            if (semaphore.getAndIncrement() != 0) {
                return
            }
            var missed = 1
            val q = this.queue
            val a = this.downstream
            val error = this.error
            do {
                while (true) {
                    if (cancelled) {
                        q.set(null)
                        return
                    }
                    if (error.get() != null) {
                        q.set(null)
                        DisposableHelper.dispose(upstream)
                        DisposableHelper.dispose(other)
                        error.tryTerminateConsumer(a)
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
