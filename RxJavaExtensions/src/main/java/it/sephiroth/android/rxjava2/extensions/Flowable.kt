@file:Suppress("unused")

package it.sephiroth.android.rxjava2.extensions


import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 06.01.21 - 13:31
 */


/**
 * alias for Flowable.observeOn(AndroidSchedulers.mainThread())
 */
fun <T> Flowable<T>.observeMain(): Flowable<T> {
    return observeOn(AndroidSchedulers.mainThread())
}

/**
 * Returns a Flowable that skips all items emitted by the source emitter
 * until a specified interval passed.
 *
 */
fun <T : Any> Flowable<T>.skipBetween(time: Long, unit: TimeUnit): Flowable<T> {
    var t: Long = 0
    return this.filter {
        val elapsed = (System.currentTimeMillis() - t)
        if (elapsed > unit.toMillis(time)) {
            t = System.currentTimeMillis()
            true
        } else {
            false
        }
    }
}
