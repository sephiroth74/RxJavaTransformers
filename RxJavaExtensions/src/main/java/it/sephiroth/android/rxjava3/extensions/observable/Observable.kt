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

package it.sephiroth.android.rxjava3.extensions.observable

import android.annotation.SuppressLint
import android.util.Log
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.annotations.CheckReturnValue
import io.reactivex.rxjava3.annotations.SchedulerSupport
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.functions.BiFunction
import io.reactivex.rxjava3.functions.Function
import io.reactivex.rxjava3.kotlin.Observables
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import it.sephiroth.android.rxjava3.extensions.MuteException
import it.sephiroth.android.rxjava3.extensions.RetryException
import it.sephiroth.android.rxjava3.extensions.observers.AutoDisposableObserver
import it.sephiroth.android.rxjava3.extensions.operators.ObservableMapNotNull
import it.sephiroth.android.rxjava3.extensions.operators.ObservableTransformers
import it.sephiroth.android.rxjava3.extensions.single.firstInList
import java.util.Objects
import java.util.concurrent.TimeUnit
import java.util.function.Predicate


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 06.01.21 - 13:35
 */


/**
 * Converts an [Observable] into a [Single]
 */
fun <T> Observable<T>.toSingle(): Single<T> where T : Any {
    return this.firstOrError()
}


/**
 * If the original [Observable] returns a [List] of items, this transformer will
 * convert the Observable into a [Maybe] which emit the very first item of the list,
 * if the list contains at least one element.
 */
fun <T : Any> Observable<List<T>>.firstInList(): Maybe<T> {
    return this.toSingle().firstInList()
}

/**
 * If the original [Observable] returns a [List] of items, this transformer will
 * convert the Observable into a [Maybe] which emit the very first item that match the predicate.
 *
 * @since 3.0.5
 */
fun <T : Any> Observable<List<T>>.firstInList(predicate: Predicate<T>): Maybe<T> {
    return this.toSingle().firstInList(predicate)
}


/**
 * Subscribe the source using an instance of the [AutoDisposableObserver].
 * The source will be disposed when a complete or error event is received.
 */
fun <T> Observable<T>.autoSubscribe(observer: AutoDisposableObserver<T>): AutoDisposableObserver<T> where T : Any {
    return this.subscribeWith(observer)
}

/**
 * @see [autoSubscribe]
 */
fun <T> Observable<T>.autoSubscribe(builder: (AutoDisposableObserver<T>.() -> Unit)): AutoDisposableObserver<T> where T : Any {
    return this.subscribeWith(AutoDisposableObserver(builder))
}

/**
 * alias for Observable.observeOn(AndroidSchedulers.mainThread())
 */
fun <T> Observable<T>.observeMain(): Observable<T> where T : Any {
    return observeOn(AndroidSchedulers.mainThread())
}

/**
 * Retries the source observable when the predicate succeeds.
 *
 * @param predicate when the predicate returns true a new attempt will be made from the source observable
 * @param maxRetry maximum number of attempts
 * @param delayBeforeRetry minimum time (see [timeUnit]) before the next attempt
 * @param timeUnit time unit for the [delayBeforeRetry] param
 */
fun <T> Observable<T>.retry(
    predicate: (Throwable) -> Boolean,
    maxRetry: Int,
    delayBeforeRetry: Long,
    timeUnit: TimeUnit
): Observable<T> where T : Any =
    retryWhen { observable ->
        Observables.zip(
            observable.map { if (predicate(it)) it else throw it },
            Observable.interval(delayBeforeRetry, timeUnit)
        ).map { if (it.second >= maxRetry) throw it.first }
    }

/**
 * Returns an Observable that emits the source observable every [time]. The source observable is triggered immediately
 * and all the consecutive calls after the time specified
 */
fun <T> Observable<T>.refreshEvery(
    time: Long,
    timeUnit: TimeUnit,
    scheduler: Scheduler = Schedulers.computation()
): Observable<T> where T : Any =
    Observable.interval(0, time, timeUnit, scheduler).flatMap { this }

/**
 * Returns an Observable that emits the source observable every time the [publisher] observable emits true
 */
fun <T> Observable<T>.autoRefresh(publisher: Observable<Boolean>): Observable<T> where T : Any {
    return publisher.filter { it }.flatMap { this }
}


/**
 * Converts the elements of a list of an Observable
 */
@CheckReturnValue
@SchedulerSupport(SchedulerSupport.NONE)
fun <R, T> Observable<List<T>>.mapList(mapper: Function<in T, out R>): Observable<List<R>> where T : Any, R : Any {
    return this.map { list -> list.map { mapper.apply(it) } }
}

/**
 * Mute the source [Observable] until the predicate [func] returns true, retrying using the given [delay]
 */
fun <T> Observable<T>.muteUntil(
    delay: Long,
    unit: TimeUnit,
    func: () -> Boolean
): Observable<T> where T : Any {
    return this.doOnNext { if (func()) throw MuteException() }
        .retryWhen { t: Observable<Throwable> ->
            t.flatMap { error: Throwable ->
                if (error is MuteException) Observable.timer(delay, unit)
                else Observable.error(error)
            }
        }
}

/**
 * Similar to mapNotNull of RxJava2.
 * Map the elements of the upstream Observable using the [mapper] function and
 * returns only those elements not null.
 * If all the elements returned by the mapper function are null, the upstream observable
 * will fire onComplete.
 *
 * @since 3.0.3
 *
 */
@CheckReturnValue
@SchedulerSupport(SchedulerSupport.NONE)
fun <T, R> Observable<T>.mapNotNull(mapper: java.util.function.Function<in T, R?>): Observable<R> where T : Any, R : Any {
    Objects.requireNonNull(mapper, "mapper is null")
    val o = ObservableMapNotNull(this, mapper)
    return RxJavaPlugins.onAssembly(o)
}

@SuppressLint("LogNotTimber")
fun <T> Observable<T>.debug(tag: String): Observable<T> where T : Any {
    return this
        .doOnNext { Log.v(tag, "onNext($it)") }
        .doOnError { Log.e(tag, "onError(${it.message})") }
        .doOnSubscribe { Log.v(tag, "onSubscribe()") }
        .doOnComplete { Log.v(tag, "onComplete()") }
        .doOnDispose { Log.w(tag, "onDispose()") }
}

@SuppressLint("LogNotTimber")
fun <T> Observable<T>.debugWithThread(tag: String): Observable<T> where T : Any {
    return this
        .doOnNext { Log.v(tag, "[${Thread.currentThread().name}] onNext($it)") }
        .doOnError { Log.e(tag, "[${Thread.currentThread().name}] onError(${it.message})") }
        .doOnSubscribe { Log.v(tag, "[${Thread.currentThread().name}] onSubscribe()") }
        .doOnComplete { Log.v(tag, "[${Thread.currentThread().name}] onComplete()") }
        .doOnDispose { Log.w(tag, "[${Thread.currentThread().name}] onDispose()") }
}

/**
 * Retry the source observable with a delay.
 * @param maxAttempts maximum number of attempts
 * @param predicate predicate which given the current attempt number and the source exception should return the next delay to start a new attempt.
 *                  The return value is in milliseconds
 * @throws [RetryException] when the total number of attempts have been reached
 * @since 3.0.6
 */
fun <T> Observable<T>.retryWhen(
    maxAttempts: Int,
    predicate: BiFunction<Throwable, Int, Long>
): Observable<T> where T : Any {
    return this.retryWhen { observable ->
        observable.zipWith(Observable.range(1, maxAttempts + 1)) { throwable, retryCount ->
            if (retryCount > maxAttempts) {
                throw RetryException(throwable)
            } else {
                predicate.apply(throwable, retryCount)
            }
        }.flatMap { delay -> Observable.timer(delay, TimeUnit.MILLISECONDS) }
    }
}

fun <T : Any> Observable<T>.doOnFirst(action: (T) -> Unit): Observable<T> =
    compose(ObservableTransformers.doOnFirst { action.invoke(it) })

fun <T : Any> Observable<T>.doAfterFirst(action: (T) -> Unit): Observable<T> =
    compose(ObservableTransformers.doAfterFirst { action.invoke(it) })

