package it.sephiroth.android.rxjava3.extensions.observers

import androidx.annotation.CallSuper
import io.reactivex.rxjava3.core.FlowableSubscriber
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper
import io.reactivex.rxjava3.internal.util.EndConsumerHelper
import org.reactivestreams.Subscription
import java.util.concurrent.atomic.AtomicReference

/*
* Copyright (c) 2016-present, RxJava Contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
* compliance with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software distributed under the License is
* distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
* the License for the specific language governing permissions and limitations under the License.
*/

/**
 * An abstract Subscriber that allows asynchronous, external cancellation by implementing [Disposable].
 *
 *
 * All pre-implemented final methods are thread-safe.
 *
 *
 * The default [.onStart] requests [Long.MAX_VALUE] by default. Override
 * the method to request a custom *positive* amount. Use the protected [.request]
 * to request more items and [.cancel] to cancel the sequence from within an
 * `onNext` implementation.
 *
 *
 * Note that calling [.request] from [.onStart] may trigger
 * an immediate, asynchronous emission of data to [.onNext]. Make sure
 * all initialization happens before the call to `request()` in `onStart()`.
 * Calling [.request] inside [.onNext] can happen at any time
 * because by design, `onNext` calls from upstream are non-reentrant and non-overlapping.
 *
 *
 * Like all other consumers, `DisposableSubscriber` can be subscribed only once.
 * Any subsequent attempt to subscribe it to a new source will yield an
 * [IllegalStateException] with message `"It is not allowed to subscribe with a(n) <class name> multiple times."`.
 *
 *
 * Implementation of [.onStart], [.onNext], [.onError]
 * and [.onComplete] are not allowed to throw any unchecked exceptions.
 * If for some reason this can't be avoided, use [io.reactivex.rxjava3.core.Flowable.safeSubscribe]
 * instead of the standard `subscribe()` method.
 *
 *
 * Example<pre>`
 * Disposable d =
 * Flowable.range(1, 5)
 * .subscribeWith(new DisposableSubscriber<Integer>() {
 * &#64;Override public void onStart() {
 * request(1);
 * }
 * &#64;Override public void onNext(Integer t) {
 * if (t == 3) {
 * cancel();
 * }
 * System.out.println(t);
 * request(1);
 * }
 * &#64;Override public void onError(Throwable t) {
 * t.printStackTrace();
 * }
 * &#64;Override public void onComplete() {
 * System.out.println("Done!");
 * }
 * });
 * // ...
 * d.dispose();
`</pre> *
 * @param <T> the received value type.
</T> */
abstract class DisposableSubscriber<T> : FlowableSubscriber<T>, Disposable where T : Any {
    val upstream: AtomicReference<Subscription> = AtomicReference()

    override fun onSubscribe(s: Subscription) {
        if (EndConsumerHelper.setOnce(this.upstream, s, javaClass)) {
            onStart()
        }
    }

    /**
     * Called once the single upstream [Subscription] is set via [.onSubscribe].
     */
    @CallSuper
    protected open fun onStart() {
        upstream.get().request(Long.MAX_VALUE)
    }

    protected abstract fun onDispose()

    /**
     * Requests the specified amount from the upstream if its [Subscription] is set via
     * onSubscribe already.
     *
     * Note that calling this method before a [Subscription] is set via [.onSubscribe]
     * leads to [NullPointerException] and meant to be called from inside [.onStart] or
     * [.onNext].
     * @param n the request amount, positive
     */
    @CallSuper
    protected fun request(n: Long) {
        upstream.get().request(n)
    }

    /**
     * Cancels the Subscription set via [.onSubscribe] or makes sure a
     * [Subscription] set asynchronously (later) is cancelled immediately.
     *
     * This method is thread-safe and can be exposed as a public API.
     */
    @CallSuper
    protected fun cancel() {
        dispose()
    }

    override fun isDisposed(): Boolean {
        return upstream.get() === SubscriptionHelper.CANCELLED
    }

    @CallSuper
    override fun dispose() {
        if (SubscriptionHelper.cancel(upstream)) {
            onDispose()
        }
    }
}
