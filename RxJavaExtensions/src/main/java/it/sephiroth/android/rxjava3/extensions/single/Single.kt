@file:Suppress("unused")

package it.sephiroth.android.rxjava3.extensions.single

import android.annotation.SuppressLint
import android.util.Log
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.annotations.CheckReturnValue
import io.reactivex.rxjava3.annotations.SchedulerSupport
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.functions.Function
import it.sephiroth.android.rxjava3.extensions.observers.AutoDisposableSingleObserver


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 06.01.21 - 13:40
 */

/**
 * If the [Single] returns a [List] of items, this transformer will
 * convert the source Single into a [Maybe] which will emit the very first item of the list,
 * if the list contains at least one element.
 */
fun <T> Single<List<T>>.firstInList(): Maybe<T> {
    return this.filter { it.isNotEmpty() }.map { it.first() }
}

/**
 * Subscribe to this [Single] using an instance of the [AutoDisposableSingleObserver]
 */
fun <T> Single<T>.autoSubscribe(observer: AutoDisposableSingleObserver<T>): AutoDisposableSingleObserver<T> where T : Any {
    return this.subscribeWith(observer)
}

/**
 * @see [autoSubscribe]
 */
fun <T> Single<T>.autoSubscribe(builder: (AutoDisposableSingleObserver<T>.() -> Unit)): AutoDisposableSingleObserver<T> where T : Any {
    return this.subscribeWith(AutoDisposableSingleObserver(builder))
}

/**
 * @see [autoSubscribe]
 */
fun <T> Single<T>.autoSubscribe(): AutoDisposableSingleObserver<T> where T : Any {
    return this.autoSubscribe(AutoDisposableSingleObserver())
}

/**
 * alias for <code>Single.observeOn(AndroidSchedulers.mainThread())</code>
 */
fun <T> Single<T>.observeMain(): Single<T> where T : Any {
    return observeOn(AndroidSchedulers.mainThread())
}


/**
 * Converts the elements of a collection of a Single using the given mapper function
 */
@CheckReturnValue
@SchedulerSupport(SchedulerSupport.NONE)
fun <R, T> Single<List<T>>.mapList(mapper: Function<in T, out R>): Single<List<R>> where T : Any, R : Any {
    return this.map { list -> list.map { mapper.apply(it) } }
}

/**
 * Flatten the list emitted by a Single to an Observable which emits each single item in the list separately
 *
 * @since 3.0.5
 */
@Suppress("UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS")
fun <T> Single<List<T>>.asObservable(): Observable<T> {
    return this.flatMapObservable { list ->
        Observable.create<T> { emitter ->
            list.mapNotNull { it }.forEach { if (!emitter.isDisposed) emitter.onNext(it) }
            if (!emitter.isDisposed) emitter.onComplete()
        }
    }
}


/**
 * Enable debug logs from a [Single], emitting
 * onNext, onError, onSubscribe and onComplete
 */
@SuppressLint("LogNotTimber")
fun <T> Single<T>.debug(tag: String): Single<T> where T : Any {
    return this
        .doOnError { Log.e(tag, "onError(${it.message})") }
        .doOnSubscribe { Log.v(tag, "onSubscribe()") }
        .doOnSuccess { Log.v(tag, "onSuccess()") }
        .doOnDispose { Log.w(tag, "onDispose()") }
}

@SuppressLint("LogNotTimber")
fun <T> Single<T>.debugWithThread(tag: String): Single<T> where T : Any {
    return this
        .doOnError { Log.e(tag, "[${Thread.currentThread().name}] onError(${it.message})") }
        .doOnSubscribe { Log.v(tag, "[${Thread.currentThread().name}] onSubscribe()") }
        .doOnSuccess { Log.v(tag, "[${Thread.currentThread().name}] onSuccess()") }
        .doOnDispose { Log.w(tag, "[${Thread.currentThread().name}] onDispose()") }
}
