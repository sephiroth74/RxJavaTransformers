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

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.functions.Function
import io.reactivex.rxjava3.internal.observers.BasicFuseableObserver


/**
 * @author Alessandro Crugnola on 18.12.21 - 21:03
 */
class ObservableMapNotNull<T : Any, R : Any>(
    private val source: Observable<T>,
    private val mapper: Function<in T, out R?>
) : Observable<R>() {

    override fun subscribeActual(observer: Observer<in R>) {
        source.subscribe(MapNotNullObserver(observer, mapper))
    }

    internal class MapNotNullObserver<T, R>(
        downstream: Observer<in R>,
        private val mapper: Function<in T, out R?>
    ) :
        BasicFuseableObserver<T, R>(downstream) where T : Any, R : Any? {
        override fun onNext(t: T) {
            if (done) {
                return
            }

            if (sourceMode != NONE) {
                downstream.onNext(null)
                return
            }

            val result: R? = try {
                mapper.apply(t)
            } catch (ex: Throwable) {
                fail(ex)
                return
            }
            if (null != result) {
                downstream.onNext(result)
            }
        }

        override fun requestFusion(mode: Int): Int {
            return transitiveBoundaryFusion(mode)
        }

        @Throws(Throwable::class)
        override fun poll(): R? {
            while (true) {
                val item = qd.poll() ?: return null
                val result = mapper.apply(item)
                if (null != result) {
                    return result
                }
            }
        }
    }
}
