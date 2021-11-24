@file:Suppress("unused")

package it.sephiroth.android.rxjava2.extensions.observable

import android.util.Log
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.annotations.CheckReturnValue
import io.reactivex.annotations.SchedulerSupport
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import it.sephiroth.android.rxjava2.extensions.MuteException
import it.sephiroth.android.rxjava2.extensions.observers.AutoDisposableObserver
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 06.01.21 - 13:35
 */


/**
 * Converts an [Observable] into a [Single]
 */
fun <T> Observable<T>.toSingle(): Single<T> {
    return this.firstOrError()
}


/**
 * If the original [Observable] returns a [List] of items, this transformer will
 * convert the Observable into a [Maybe] which emit the very first item of the list,
 * if the list contains at least one element.
 */
fun <T> Observable<List<T>>.firstInList(): Maybe<T> {
    return this.firstOrError().filter { it.isNotEmpty() }.map { it.first() }
}


/**
 * Subscribe the source using an instance of the [AutoDisposableObserver].
 * The source will be disposed when a complete or error event is received.
 */
fun <T> Observable<T>.autoSubscribe(observer: AutoDisposableObserver<T>): AutoDisposableObserver<T> {
    return this.subscribeWith(observer)
}

/**
 * @see [autoSubscribe]
 */
fun <T> Observable<T>.autoSubscribe(builder: (AutoDisposableObserver<T>.() -> Unit)): AutoDisposableObserver<T> {
    return this.subscribeWith(AutoDisposableObserver(builder))
}

/**
 * alias for Observable.observeOn(AndroidSchedulers.mainThread())
 */
fun <T> Observable<T>.observeMain(): Observable<T> {
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
): Observable<T> =
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
): Observable<T> =
    Observable.interval(0, time, timeUnit, scheduler).flatMap { this }

/**
 * Returns an Observable that emits the source observable every time the [publisher] observable emits true
 */
fun <T> Observable<T>.autoRefresh(publisher: Observable<Boolean>): Observable<T> {
    return publisher.filter { it }.flatMap { this }
}


/**
 * Converts the elements of a list of an Observable
 */
@CheckReturnValue
@SchedulerSupport(SchedulerSupport.NONE)
fun <R, T> Observable<List<T>>.mapList(mapper: io.reactivex.functions.Function<in T, out R>): Observable<List<R>> {
    return this.map { list -> list.map { mapper.apply(it) } }
}

/**
 * Mute the source [Observable] until the predicate [func] returns true, retrying using the given [delay]
 */
fun <T> Observable<T>.muteUntil(delay: Long, unit: TimeUnit, func: () -> Boolean): Observable<T> {
    return this.doOnNext { if (func()) throw MuteException() }
        .retryWhen { t: Observable<Throwable> ->
            t.flatMap { error: Throwable ->
                if (error is MuteException) Observable.timer(delay, unit)
                else Observable.error(error)
            }
        }
}

fun count(
    start: Long,
    end: Long,
    step: Long,
    unit: TimeUnit,
    onTick: ((Long) -> Unit),
    onComplete: (() -> Unit)?
): Disposable {
    require(start != end) { "start != end required but it was $start, $end" }
    require(step <= (end - start).absoluteValue) { "step is bigger than the time span" }
    val reversed = start > end
    val beginning = if (reversed) end else start
    val ending = (if (reversed) start else end) + 1
    val total = ending - beginning

    return Observable.intervalRange(0, total, 0L, 1, unit)
        .doOnNext { }
        .filter { value ->
            value % step == 0L || value == ending
        }
        .map { value ->
            if (reversed) start - value else value
        }.subscribe(
            { value ->
                onTick.invoke(value)

                val completed = value == end
                if (completed) onComplete?.invoke()
            },
            { /** empty **/ })
}


fun <T> Observable<T>.debug(tag: String): Observable<T> {
    return this
        .doOnNext { Log.v(tag, "onNext($it)") }
        .doOnError { Log.e(tag, "onError(${it.message})") }
        .doOnSubscribe { Log.v(tag, "onSubscribe()") }
        .doOnComplete { Log.v(tag, "onComplete()") }
        .doOnDispose { Log.w(tag, "onDispose()") }
}

fun <T> Observable<T>.debugWithThread(tag: String): Observable<T> {
    return this
        .doOnNext { Log.v(tag, "[${Thread.currentThread().name}] onNext($it)") }
        .doOnError { Log.e(tag, "[${Thread.currentThread().name}] onError(${it.message})") }
        .doOnSubscribe { Log.v(tag, "[${Thread.currentThread().name}] onSubscribe()") }
        .doOnComplete { Log.v(tag, "[${Thread.currentThread().name}] onComplete()") }
        .doOnDispose { Log.w(tag, "[${Thread.currentThread().name}] onDispose()") }
}

