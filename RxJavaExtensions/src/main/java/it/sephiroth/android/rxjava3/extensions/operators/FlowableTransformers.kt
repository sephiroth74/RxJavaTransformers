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

import io.reactivex.rxjava3.annotations.BackpressureKind
import io.reactivex.rxjava3.annotations.BackpressureSupport
import io.reactivex.rxjava3.annotations.SchedulerSupport
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableTransformer
import io.reactivex.rxjava3.internal.functions.ObjectHelper
import org.reactivestreams.Publisher
import java.util.*


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 16.12.21 - 11:13
 */
object FlowableTransformers {

    /**
     * Relays values until the other Publisher signals false and resumes if the other
     * Publisher signals true again, like closing and opening a valve and not losing
     * any items from the main source.
     * When resumed, only the last value published from the source, if any, will be delivered (and all the following ones)
     *
     * Properties:
     *
     *  * The operator starts with an open valve.
     *  * If the other Publisher completes, the sequence terminates with an `IllegalStateException`.
     *  * The operator doesn't run on any particular [Scheduler][io.reactivex.rxjava3.core.Scheduler].
     *  * The operator is a pass-through for backpressure and uses an internal unbounded buffer
     *
     * @param <T> the value type of the main source
     * @param other the other source
     * @return the new FlowableTransformer instance
     * @throws NullPointerException if `other` is null
     *
     * @since 3.0.1
    </T> */
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    fun <T> valveLast(other: Publisher<Boolean>): FlowableTransformer<T, T> where T : Any {
        return valveLast(other, true, Flowable.bufferSize())
    }

    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    fun <T> valveLast(other: Publisher<Boolean>, defaultOpen: Boolean): FlowableTransformer<T, T> where T : Any {
        return valveLast(other, defaultOpen, Flowable.bufferSize())
    }

    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    fun <T> valveLast(other: Publisher<Boolean>, defaultOpen: Boolean, bufferSize: Int): FlowableTransformer<T, T> where T : Any {
        Objects.requireNonNull(other, "other is null")
        ObjectHelper.verifyPositive(bufferSize, "bufferSize")
        return FlowableLastValve(null, other, defaultOpen, bufferSize)
    }
}
