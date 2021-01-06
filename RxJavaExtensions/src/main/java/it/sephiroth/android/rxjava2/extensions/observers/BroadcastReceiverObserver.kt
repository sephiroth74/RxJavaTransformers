@file:Suppress("unused")

package it.sephiroth.android.rxjava2.extensions.observers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.disposables.Disposables


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 06.01.21 - 13:41
 */

class BroadcastReceiverObserver(val emitter: ObservableEmitter<Intent>) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        emitter.onNext(intent)
    }
}

/**
 * Register a new broadcast receiver from a given a list of actions.
 * The [BroadcastReceiver] it's unregistered once the observable is disposed
 */
fun Context.observeBroadcasts(vararg action: String): Observable<Intent> {
    val filter = IntentFilter()
    action.forEach { filter.addAction(it) }
    return observeBroadcasts(filter)
}

@Suppress("TooGenericExceptionCaught")
fun Context.observeBroadcasts(intentFilter: IntentFilter): Observable<Intent> {
    return Observable.create { observer ->
        var receiver: BroadcastReceiverObserver? = BroadcastReceiverObserver(observer)
        observer.setDisposable(Disposables.fromRunnable {
            receiver?.let {
                try {
                    unregisterReceiver(it)
                } catch (t: Throwable) {
                    t.printStackTrace()
                }; receiver = null
            }
        })
        registerReceiver(receiver, intentFilter)
    }
}
