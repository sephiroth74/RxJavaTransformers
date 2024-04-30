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

import io.reactivex.rxjava3.annotations.SchedulerSupport
import io.reactivex.rxjava3.core.ObservableSource
import io.reactivex.rxjava3.core.ObservableTransformer
import io.reactivex.rxjava3.functions.Function
import java.util.Objects


/**
 * @author Alessandro Crugnola on 05.01.21 - 23:00
 */
object ObservableTransformers {

    /**
     * Relays values until the other ObservableSource signals false and resumes if the other
     * ObservableSource signals true again, like closing and opening a valve and not losing
     * any items from the main source.
     * When resumed, only the last value emitted by the source observable, if any.
     * <p>Properties:
     * <ul>
     * <li>The operator starts with an open valve.</li>
     * <li>If the other ObservableSource completes, the sequence terminates with an {@code IllegalStateException}.</li>
     * <li>The operator doesn't run on any particular {@link io.reactivex.Scheduler Scheduler}.</li>
     * </ul>
     * @param <T> the value type of the main source
     * @param other the other source
     * @return the new ObservableTransformer instance
     * @throws NullPointerException if {@code other} is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    fun <T> valveLast(other: ObservableSource<Boolean>): ObservableTransformer<T, T> where T : Any {
        return valveLast(other, true)
    }

    @SchedulerSupport(SchedulerSupport.NONE)
    fun <T> valveLast(
        other: ObservableSource<Boolean>,
        defaultOpen: Boolean = true
    ): ObservableTransformer<T, T> where T : Any {
        Objects.requireNonNull(other, "other is null")
        return ObservableLastValve(other, defaultOpen)
    }


    @SchedulerSupport(SchedulerSupport.NONE)
    fun <T : Any> doOnNth(nth: Long, action: Function<T, Unit>): ObservableTransformer<T, T> {
        return ObservableTransformer { upstream -> upstream.lift(ObservableOperatorDoOnNth(action, nth)) }
    }

    @SchedulerSupport(SchedulerSupport.NONE)
    fun <T : Any> doAfterNth(
        nth: Long,
        action: Function<T, Unit>
    ): ObservableTransformer<T, T> {
        return ObservableTransformer { upstream ->
            upstream.lift(
                ObservableOperatorDoAfterNth(
                    action,
                    nth
                )
            )
        }
    }

    fun <T : Any> doOnFirst(action: Function<T, Unit>): ObservableTransformer<T, T> {
        return ObservableTransformer { upstream -> upstream.lift(ObservableOperatorDoOnNth(action, 0)) }
    }

    fun <T : Any> doAfterFirst(action: Function<T, Unit>): ObservableTransformer<T, T> {
        return ObservableTransformer { upstream ->
            upstream.lift(
                ObservableOperatorDoAfterNth(
                    action,
                    0
                )
            )
        }
    }

}
