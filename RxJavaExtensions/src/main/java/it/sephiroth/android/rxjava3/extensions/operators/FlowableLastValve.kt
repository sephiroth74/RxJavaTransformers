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

package it.sephiroth.android.rxjava3.extensions.operators

import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableOperator
import io.reactivex.rxjava3.core.FlowableSubscriber
import io.reactivex.rxjava3.core.FlowableTransformer
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper
import io.reactivex.rxjava3.internal.util.AtomicThrowable
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Allows stopping and resuming the flow of the main source when a secondary flow
 * signals false and true respectively.
 * Once the secondary flow signal true only the flow will restart and the last missed value
 * will be sent
 *
 * @param <T> the main source's value type
 * @since 3.0.1
</T> */
internal class FlowableLastValve<T>(
    private val source: Publisher<out T>?,
    private val other: Publisher<Boolean>,
    private val defaultOpen: Boolean,
    private val bufferSize: Int
) : Flowable<T>(), FlowableOperator<T, T>,
    FlowableTransformer<T, T> where T : Any {
    override fun subscribeActual(s: Subscriber<in T>) {
        source?.subscribe(apply(s))
    }

    override fun apply(subscriber: Subscriber<in T>): Subscriber<in T> {
        val parent = ValveMainSubscriber(subscriber, defaultOpen)
        subscriber.onSubscribe(parent)
        other.subscribe(parent.other)
        return parent
    }

    override fun apply(upstream: Flowable<T>): Publisher<T> {
        return FlowableLastValve(upstream, other, defaultOpen, bufferSize)
    }

    internal class ValveMainSubscriber<T>(private val downstream: Subscriber<in T>, defaultOpen: Boolean) : Subscriber<T>, Subscription {
        private val semaphore = AtomicInteger()
        private val upstream: AtomicReference<Subscription> = AtomicReference()
        private val requested: AtomicLong = AtomicLong()
        private val queue: AtomicReference<T?> = AtomicReference()
        val other: OtherSubscriber = OtherSubscriber()
        private val error: AtomicThrowable = AtomicThrowable()

        @Volatile
        var done = false

        @Volatile
        var gate: Boolean = defaultOpen

        @Volatile
        var cancelled = false
        override fun onSubscribe(s: Subscription) {
            SubscriptionHelper.deferredSetOnce(upstream, requested, s)
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

        override fun request(n: Long) {
            SubscriptionHelper.deferredRequest(upstream, requested, n)
        }

        override fun cancel() {
            cancelled = true
            SubscriptionHelper.cancel(upstream)
            SubscriptionHelper.cancel(other)
            error.tryTerminateAndReport()
        }

        private fun drain() {
            if (semaphore.andIncrement != 0) {
                return
            }
            var missed = 1
            val q = queue
            val a = downstream
            val error = error
            while (true) {
                while (true) {
                    if (cancelled) {
                        q.set(null)
                        return
                    }
                    if (error.get() != null) {
                        q.set(null)
                        SubscriptionHelper.cancel(upstream)
                        SubscriptionHelper.cancel(other)
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
                        SubscriptionHelper.cancel(other)
                        a.onComplete()
                        return
                    }
                    if (empty) {
                        break
                    }
                    a.onNext(v)
                }
                missed = semaphore.addAndGet(-missed)
                if (missed == 0) {
                    break
                }
            }
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

        internal inner class OtherSubscriber : AtomicReference<Subscription?>(), FlowableSubscriber<Boolean> {
            override fun onSubscribe(s: Subscription) {
                if (SubscriptionHelper.setOnce(this, s)) {
                    s.request(Long.MAX_VALUE)
                }
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
