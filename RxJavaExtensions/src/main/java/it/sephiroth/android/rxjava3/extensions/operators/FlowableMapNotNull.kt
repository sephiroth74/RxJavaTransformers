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
import io.reactivex.rxjava3.internal.subscribers.BasicFuseableSubscriber
import io.reactivex.rxjava3.operators.ConditionalSubscriber
import org.reactivestreams.Subscriber

class FlowableMapNotNull<T : Any, R : Any>(val source: Flowable<T>, val mapper: java.util.function.Function<in T, out R?>) : Flowable<R>() {
    override fun subscribeActual(s: Subscriber<in R>) {
        source.subscribe(MapOptionalSubscriber(s, mapper))
    }

    internal class MapOptionalSubscriber<T : Any, R>(downstream: Subscriber<in R>?, val mapper: java.util.function.Function<in T, out R?>) :
            BasicFuseableSubscriber<T, R>(downstream), ConditionalSubscriber<T> {
        override fun onNext(t: T) {
            if (!tryOnNext(t)) {
                upstream.request(1)
            }
        }

        override fun tryOnNext(t: T): Boolean {
            if (done) {
                return true
            }
            if (sourceMode != NONE) {
                downstream.onNext(null)
                return true
            }

            val result = try {
                mapper.apply(t)
            } catch (ex: Throwable) {
                fail(ex)
                return true
            }

            if (null != result) {
                downstream.onNext(result)
                return true
            }
            return false
        }

        override fun requestFusion(mode: Int): Int {
            return transitiveBoundaryFusion(mode)
        }

        @Throws(Throwable::class)
        override fun poll(): R? {
            while (true) {
                val item = qs.poll() ?: return null
                val result = mapper.apply(item)
                if (null != result) {
                    return result
                }
                if (sourceMode == ASYNC) {
                    qs.request(1)
                }
            }
        }
    }
}
