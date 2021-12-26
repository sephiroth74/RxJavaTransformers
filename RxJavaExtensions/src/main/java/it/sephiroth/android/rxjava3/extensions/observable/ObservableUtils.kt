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

package it.sephiroth.android.rxjava3.extensions.observable

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableSource
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.absoluteValue
import kotlin.math.ceil


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 24.11.21 - 13:56
 */
@Suppress("unused")
object ObservableUtils {

    /**
     * Starts a timer which will count up to [end] (converted to [endUnit]), and producing
     * a tick interval every [step] (converted to [stepUnit]).
     *
     * For instance `(timer, 1, TimeUnit.SECONDS, 10, TimeUnit.MILLISECONDS)` will invoke a timer
     * interval 100 times, every 10 milliseconds.
     *
     */
    fun timer(
        end: Long,
        endUnit: TimeUnit,
        step: Long,
        stepUnit: TimeUnit,
        onTick: ((Long, Long) -> Unit)?,
        onComplete: (() -> Unit)?
    ): Disposable {
        val endTime = stepUnit.convert(end, endUnit)
        require(endTime > step) { "step time must be < than end time" }
        val totalSteps = ceil((endTime.toDouble()) / step).toLong()

        return Observable
            .interval(step, stepUnit).take(totalSteps)
            .observeMain()
            .autoSubscribe {
                doOnNext {
                    // onNext
                    val currentStep = it + 1
                    val time = currentStep * step
                    onTick?.invoke(currentStep, time)
                }
                doOnComplete {
                    // onComplete
                    onComplete?.invoke()
                }
            }
    }

    /**
     * Starts a timer which will be report to [onTick] listener on each timer tick. <br />
     * [onComplete] is called once the timer ends. <br />
     * Both [onTick] and [onComplete] will be invoked from the main thread. <br />
     * For instance `ObservableUtils.countDown(0, 10, 1, TimeUnit.SECONDS, listener1, listener2)` will
     * invoke listener1 every 1 second for 10 times (from 0 to 10)
     *
     * @param start the timer start value
     * @param end the timer end value
     * @param step defines every how many steps the [onTick] should be invoked
     * @param unit TimeUnit
     */
    @JvmStatic
    fun countDown(
        start: Long,
        end: Long,
        step: Long,
        unit: TimeUnit,
        onTick: ((Long) -> Unit)?,
        onComplete: (() -> Unit)?
    ): Disposable {
        require(start != end) { "start != end required but it was $start, $end" }
        require(step <= (end - start).absoluteValue) { "step is bigger than the time span" }
        val reversed = start > end
        val beginning = if (reversed) end else start
        val ending = (if (reversed) start else end) + 1
        val total = ending - beginning

        return Observable.intervalRange(0, total, 0L, 1, unit)
            .filter { value ->
                value % step == 0L || value == ending
            }
            .map { value ->
                if (reversed) start - value else value
            }
            .observeMain()
            .autoSubscribe {
                doOnNext { value ->
                    onTick?.invoke(value)
                    val completed = value == end
                    if (completed) onComplete?.invoke()
                }
            }
    }

    /**
     * Creates a pausable Observable which observes the items emitted from the [pausedSource] source
     * to internally pause the emission of the resulting observable
     */
    @JvmStatic
    fun pausedTimer(delay: Long, unit: TimeUnit, pausedSource: Observable<Boolean>): Observable<Long> {

        val startTime = AtomicLong()
        val elapsedTime = AtomicLong()
        val delayTime = AtomicLong(unit.toMillis(delay))

        return pausedSource.doOnSubscribe {
            startTime.set(System.currentTimeMillis())
            elapsedTime.set(unit.toMillis(delay))
        }
            .distinctUntilChanged()
            .takeWhile { elapsedTime.get() > 0 }
            .switchMap { paused ->
                val now = System.currentTimeMillis()
                if (!paused) {
                    // resumed
                    startTime.set(now)
                    return@switchMap Observable.timer(elapsedTime.get(), TimeUnit.MILLISECONDS)
                } else {
                    // paused
                    val elapsed = now - startTime.get()
                    val newDelay = delayTime.get() - elapsed
                    delayTime.set(newDelay)
                    elapsedTime.set(newDelay)
                    startTime.set(now)
                    return@switchMap Observable.never()
                }
            }.map { System.currentTimeMillis() }.take(1)
    }

    /**
     * Returns a pausable [Observable.interval] which emits the value emitted by the source
     * Observable.interval while the [paused] observable has no value or emits true.
     */
    @JvmStatic
    fun pausedInterval(
        delay: Long,
        unit: TimeUnit,
        paused: ObservableSource<Boolean>,
    ): Observable<Long> {
        return this.pausedInterval(delay, unit, paused, Schedulers.computation())
    }

    @JvmStatic
    fun pausedInterval(
        delay: Long,
        unit: TimeUnit,
        paused: ObservableSource<Boolean>,
        scheduler: Scheduler
    ): Observable<Long> {
        val elapsedTime = AtomicLong()
        return Observable.interval(delay, unit, scheduler)
            .withLatestFrom(paused) { _, t2 -> !t2 }
            .filter { it }
            .map { elapsedTime.addAndGet(unit.toMillis(delay)) }
    }
}
