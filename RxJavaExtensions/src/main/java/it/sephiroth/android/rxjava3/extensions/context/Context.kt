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

package it.sephiroth.android.rxjava3.extensions.context

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.disposables.Disposable
import it.sephiroth.android.rxjava3.extensions.core.BroadcastReceiverObservableOnSubscribe
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
@Deprecated(
    message = "Use observeBroadcast instead",
    replaceWith = ReplaceWith("observeBroadcast(intentFilter, permission, receiverFlags)")
)
fun Context.observeBroadcasts(vararg action: String): Observable<Intent> {
    return observeBroadcasts(*action, dataScheme = null, permission = null, scheduler = null)
}

@Deprecated(
    message = "Use observeBroadcast instead",
    replaceWith = ReplaceWith("observeBroadcast(intentFilter, permission, receiverFlags)")
)
fun Context.observeBroadcasts(
    vararg action: String,
    dataScheme: String? = null,
    permission: String? = null,
    scheduler: Handler? = null
): Observable<Intent> {
    val intentFilter = IntentFilter().also { filter ->
        action.forEach { filter.addAction(it) }
        dataScheme?.let { filter.addDataScheme(it) }
    }
    return observeBroadcasts(
        intentFilter = intentFilter,
        permission = permission,
        scheduler = scheduler
    )
}

@SuppressLint("UnspecifiedRegisterReceiverFlag")
@Deprecated(
    message = "Use observeBroadcast instead",
    replaceWith = ReplaceWith("observeBroadcast(*intentFilter, *permission, *receiverFlags)")
)
fun Context.observeBroadcasts(
    intentFilter: IntentFilter,
    permission: String? = null,
    scheduler: Handler? = null,
    receiverFlags: Int? = null,
): Observable<Intent> {
    val observable = Observable.create { observer: ObservableEmitter<Intent> ->
        var receiver: BroadcastReceiverObserver? = BroadcastReceiverObserver(observer)
        observer.setDisposable(Disposable.fromRunnable {
            receiver?.let {
                try {
                    unregisterReceiver(it)
                } catch (t: Throwable) {
                    t.printStackTrace()
                }; receiver = null
            }
        })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                /* receiver = */
                receiver,
                /* filter = */
                intentFilter,
                /* broadcastPermission = */
                permission,
                /* scheduler = */
                scheduler,
                /* flags = */
                receiverFlags ?: Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(receiver, intentFilter, permission, scheduler)
        }
    }

    return observable
        .subscribeOn(AndroidSchedulers.mainThread())
        .observeOn(AndroidSchedulers.mainThread())
}

/**
 * Create a new [Observable] to subscribe to based on an Android broadcast
 */
fun Context.observeBroadcast(
    intentFilter: IntentFilter,
    broadcastPermission: String? = null,
    schedulerHandler: Handler? = null,
    receiverFlags: Int? = null,
    registerAction: (() -> Unit)? = null,
): Observable<Intent> {
    return Observable.create(
        BroadcastReceiverObservableOnSubscribe(
            this,
            intentFilter,
            broadcastPermission,
            schedulerHandler,
            receiverFlags,
            registerAction
        )
    )
}
