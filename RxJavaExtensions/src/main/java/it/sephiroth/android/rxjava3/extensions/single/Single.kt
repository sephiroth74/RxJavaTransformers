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
import java.util.*


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 06.01.21 - 13:40
 */

/**
 * If the [Single] returns a [List] of items, this transformer will
 * convert the source into a [Maybe] which will emit the very first item of the list,
 * if the list contains at least one element.
 */
fun <T> Single<List<T>>.firstInList(): Maybe<T> {
    return this.filter { it.isNotEmpty() }.map { it.first() }
}


/**
 * If the [Single] returns a [List] of items, this transformer will
 * convert the source into a [Maybe] which will emit the very first non null item of the list.
 *
 * @since 3.0.5
 */
fun <T> Single<List<T>>.firstInListNotNull(): Maybe<T> {
    return this.filter { it.isNotEmpty() }.mapOptional { Optional.ofNullable(it.firstOrNull { item -> item != null }) }
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
 * Converts the elements of a list of a Single using the given mapper function
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
