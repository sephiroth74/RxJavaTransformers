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

package it.sephiroth.android.rxjava3.extensions.maybe

import android.annotation.SuppressLint
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
 * Subscribe to this [Maybe] source using an anonymous instance of the [AutoDisposableMaybeObserver]
 */
fun <T> Maybe<T>.autoSubscribe(): AutoDisposableMaybeObserver<T> = this.autoSubscribe(AutoDisposableMaybeObserver())

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


@SuppressLint("LogNotTimber")
fun <T> Maybe<T>.debug(tag: String): Maybe<T> {
    return this
        .doOnSuccess { Log.v(tag, "onSuccess($it)") }
        .doOnError { Log.e(tag, "onError(${it.message})") }
        .doOnSubscribe { Log.v(tag, "onSubscribe()") }
        .doOnComplete { Log.v(tag, "onComplete()") }
        .doOnDispose { Log.w(tag, "onDispose()") }
}

@SuppressLint("LogNotTimber")
fun <T> Maybe<T>.debugWithThread(tag: String): Maybe<T> {
    return this
        .doOnSuccess { Log.v(tag, "[${Thread.currentThread().name}] onSuccess()") }
        .doOnError { Log.e(tag, "[${Thread.currentThread().name}] onError(${it.message})") }
        .doOnSubscribe { Log.v(tag, "[${Thread.currentThread().name}] onSubscribe()") }
        .doOnComplete { Log.v(tag, "[${Thread.currentThread().name}] onComplete()") }
        .doOnDispose { Log.w(tag, "[${Thread.currentThread().name}] onDispose()") }
}
