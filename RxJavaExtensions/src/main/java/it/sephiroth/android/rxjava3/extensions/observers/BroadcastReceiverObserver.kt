@file:Suppress("unused")

package it.sephiroth.android.rxjava3.extensions.observers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.reactivex.rxjava3.core.ObservableEmitter


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
