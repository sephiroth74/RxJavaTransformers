@file:Suppress("unused")

package it.sephiroth.android.rxjava3.extensions.maybe

import android.util.Log
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import it.sephiroth.android.rxjava3.extensions.observers.AutoDisposableMaybeObserver


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 06.01.21 - 13:35
 */


/**
 * Subscribe to this [Maybe] using an instance of the [AutoDisposableMaybeObserver]
 */
fun <T> Maybe<T>.autoSubscribe(observer: AutoDisposableMaybeObserver<T>): AutoDisposableMaybeObserver<T> {
    return this.subscribeWith(observer)
}

/**
 * @see [autoSubscribe]
 */
fun <T> Maybe<T>.autoSubscribe(builder: (AutoDisposableMaybeObserver<T>.() -> Unit)): AutoDisposableMaybeObserver<T> {
    return this.subscribeWith(AutoDisposableMaybeObserver(builder))
}

/**
 * alias for Maybe.observeOn(AndroidSchedulers.mainThread())
 */
fun <T> Maybe<T>.observeMain(): Maybe<T> {
    return observeOn(AndroidSchedulers.mainThread())
}


fun <T> Maybe<T>.debug(tag: String): Maybe<T> {
    return this
        .doOnSuccess { Log.v(tag, "onSuccess($it)") }
        .doOnError { Log.e(tag, "onError(${it.message})") }
        .doOnSubscribe { Log.v(tag, "onSubscribe()") }
        .doOnComplete { Log.v(tag, "onComplete()") }
        .doOnDispose { Log.w(tag, "onDispose()") }
}

fun <T> Maybe<T>.debugWithThread(tag: String): Maybe<T> {
    return this
        .doOnSuccess { Log.v(tag, "[${Thread.currentThread().name}] onSuccess()") }
        .doOnError { Log.e(tag, "[${Thread.currentThread().name}] onError(${it.message})") }
        .doOnSubscribe { Log.v(tag, "[${Thread.currentThread().name}] onSubscribe()") }
        .doOnComplete { Log.v(tag, "[${Thread.currentThread().name}] onComplete()") }
        .doOnDispose { Log.w(tag, "[${Thread.currentThread().name}] onDispose()") }
}
