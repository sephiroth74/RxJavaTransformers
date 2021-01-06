@file:Suppress("unused")

package it.sephiroth.android.rxjava2.extensions

import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import it.sephiroth.android.rxjava2.extensions.observers.AutoDisposableMaybeObserver


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

