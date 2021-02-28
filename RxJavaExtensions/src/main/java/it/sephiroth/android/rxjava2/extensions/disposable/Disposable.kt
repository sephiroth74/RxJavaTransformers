@file:Suppress("unused")

package it.sephiroth.android.rxjava2.extensions.disposable

import io.reactivex.disposables.Disposable


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 28.02.21 - 18:34
 */


/**
 * Safe unsubscribe a [Disposable]
 */
fun Disposable?.disposeSafe() {
    if (this?.isDisposed == false) {
        this.dispose()
    }
}

/**
 * Returns true if a [Disposable] is currently not null and not unsubscribed
 */
fun Disposable?.isDisposed(): Boolean = this?.isDisposed ?: true

