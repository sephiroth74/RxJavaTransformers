package it.sephiroth.android.rxjava3.extensions.core

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.util.Log
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.ObservableOnSubscribe

internal class BroadcastReceiverObservableOnSubscribe(
    private val context: Context,
    private val intentFilter: IntentFilter,
    private val broadcastPermission: String? = null,
    private val schedulerHandler: Handler? = null,
    private val receiverFlags: Int? = null,
    private val onRegister: (() -> Unit)? = null,
) : ObservableOnSubscribe<Intent> {

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun subscribe(e: ObservableEmitter<Intent>) {
        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                e.onNext(intent)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.v(TAG, "registering receiver")
            context.registerReceiver(
                broadcastReceiver,
                intentFilter,
                broadcastPermission,
                schedulerHandler,
                receiverFlags ?: Context.RECEIVER_EXPORTED
            )
        } else {
            Log.v(TAG, "registering receiver")
            context.registerReceiver(
                broadcastReceiver,
                intentFilter
            )
        }

        e.setCancellable {
            Log.v(TAG, "unregistering receiver")
            context.unregisterReceiver(broadcastReceiver)
        }

        onRegister?.invoke()
    }

    companion object {
        private val TAG = BroadcastReceiverObservableOnSubscribe::class.java.simpleName
    }
}
