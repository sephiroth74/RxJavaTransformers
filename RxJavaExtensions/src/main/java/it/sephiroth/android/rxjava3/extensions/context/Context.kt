@file:Suppress("unused")

package it.sephiroth.android.rxjava3.extensions.context

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import it.sephiroth.android.rxjava3.extensions.observers.BroadcastReceiverObserver


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 28.02.21 - 18:52
 */

/**
 * Register the current context to one or more intent actions.
 * Once an action is received, the result [Observable] will trigger a new [Intent]
 */
fun Context.observeBroadcasts(vararg action: String): Observable<Intent> {
    val filter = IntentFilter()
    action.forEach { filter.addAction(it) }
    return observeBroadcasts(filter)
}

/**
 * @see [observeBroadcasts]
 */
@Suppress("TooGenericExceptionCaught")
fun Context.observeBroadcasts(intentFilter: IntentFilter): Observable<Intent> {
    val observable = Observable.create<Intent> { observer ->
        var receiver: BroadcastReceiverObserver? = BroadcastReceiverObserver(observer)
        observer.setDisposable(Disposable.fromRunnable {
            receiver?.let {
                try {
                    unregisterReceiver(it)
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
                receiver = null
            }
        })
        registerReceiver(receiver, intentFilter)
    }
    return observable
}
