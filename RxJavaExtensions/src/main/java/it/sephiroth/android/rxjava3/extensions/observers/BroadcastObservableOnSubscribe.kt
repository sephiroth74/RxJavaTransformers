package it.sephiroth.android.rxjava3.extensions.observers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.ObservableOnSubscribe
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Action
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.subjects.PublishSubject

class BroadcastObservableOnSubscribe(
    private val context: Context,
    private val filter: IntentFilter,
    private val permissions: String? = null,
    private val flags: Int? = null,
) : ObservableOnSubscribe<Intent>, BroadcastReceiver() {

    private val subject: PublishSubject<Intent> = PublishSubject.create()

    private val topic: Observable<Intent> =
        subject.doOnSubscribe(DoOnSubscribe()).doOnDispose(DoOnDispose())

    override fun subscribe(emitter: ObservableEmitter<Intent>) {
        topic.subscribe(EmitterObserver(emitter))
    }

    override fun onReceive(context: Context, intent: Intent) {
        this.subject.onNext(intent)
    }

    private fun doOnSubscribe() = registerReceiver()

    private fun doOnDispose() = unregisterReceiver()

    private fun unregisterReceiver() {
        try {
            context.unregisterReceiver(this)
        } catch (t: Throwable) {
            // empty
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerReceiver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                this,
                filter,
                permissions,
                null,
                flags ?: Context.RECEIVER_EXPORTED
            )
        } else {
            context.registerReceiver(this, filter, permissions, null)
        }
    }

    private inner class DoOnSubscribe : Consumer<Disposable> {
        override fun accept(t: Disposable) {
            doOnSubscribe()
        }
    }

    private inner class DoOnDispose : Action {
        @Throws(Exception::class)
        override fun run() {
            doOnDispose()
        }
    }

    companion object {
        private val TAG = BroadcastObservableOnSubscribe::class.java.simpleName
    }
}
