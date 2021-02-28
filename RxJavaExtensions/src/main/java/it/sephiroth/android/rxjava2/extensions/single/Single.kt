@file:Suppress("unused")

package it.sephiroth.android.rxjava2.extensions

import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import it.sephiroth.android.rxjava2.extensions.observers.AutoDisposableSingleObserver


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 06.01.21 - 13:40
 */

/**
 * If the original [Single] returns a [List] of items, this transformer will
 * convert the Observable into a [Maybe] which emit the very first item of the list,
 * if the list contains at least one element.
 */
fun <T> Single<List<T>>.firstInList(): Maybe<T> {
    return this.filter { it.isNotEmpty() }.map { it.first() }
}

/**
 * Subscribe to this [Single] using an instance of the [AutoDisposableSingleObserver]
 */
fun <T> Single<T>.autoSubscribe(observer: AutoDisposableSingleObserver<T>): AutoDisposableSingleObserver<T> {
    return this.subscribeWith(observer)
}

/**
 * @see [autoSubscribe]
 */
fun <T> Single<T>.autoSubscribe(builder: (AutoDisposableSingleObserver<T>.() -> Unit)): AutoDisposableSingleObserver<T> {
    return this.subscribeWith(AutoDisposableSingleObserver(builder))
}

/**
 * alias for Observable.observeOn(AndroidSchedulers.mainThread())
 */
fun <T> Single<T>.observeMain(): Single<T> {
    return observeOn(AndroidSchedulers.mainThread())
}

