@file:Suppress("unused")

package it.sephiroth.android.rxjava2.extensions

import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import it.sephiroth.android.rxjava2.extensions.observers.AutoDisposableObserver
import java.util.concurrent.TimeUnit


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
 * Subscribe to this [Single] using an instance of the [AutoDisposableObserver]
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

fun <T> Observable<T>.retry(predicate: (Throwable) -> Boolean, maxRetry: Int, delayBeforeRetry: Long, timeUnit: TimeUnit): Observable<T> =
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
fun <T> Observable<T>.refreshEvery(time: Long, timeUnit: TimeUnit): Observable<T> = Observable.interval(0, time, timeUnit).flatMap { this }

/**
 * Returns an Observable that emits the source observable every time the [publisher] observable emits true
 */
fun <T> Observable<T>.autoRefresh(publisher: Observable<Boolean>): Observable<T> {
    return publisher.filter { it }.flatMap { this }
}

