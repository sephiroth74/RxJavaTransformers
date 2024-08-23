package it.sephiroth.android.rxjava3.extensions.observers

import androidx.annotation.CallSuper
import io.reactivex.rxjava3.core.CompletableObserver
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.internal.disposables.DisposableHelper
import io.reactivex.rxjava3.internal.util.EndConsumerHelper
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
 * An abstract [CompletableObserver] that allows asynchronous cancellation by implementing Disposable.
 *
 *
 * All pre-implemented final methods are thread-safe.
 *
 *
 * Like all other consumers, `DisposableCompletableObserver` can be subscribed only once.
 * Any subsequent attempt to subscribe it to a new source will yield an
 * [IllegalStateException] with message `"It is not allowed to subscribe with a(n) <class name> multiple times."`.
 *
 *
 * Implementation of [.onStart], [.onError] and
 * [.onComplete] are not allowed to throw any unchecked exceptions.
 *
 *
 * Example<pre>`
 * Disposable d =
 * Completable.complete().delay(1, TimeUnit.SECONDS)
 * .subscribeWith(new DisposableMaybeObserver<Integer>() {
 * &#64;Override public void onStart() {
 * System.out.println("Start!");
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
 */
abstract class DisposableCompletableObserver : CompletableObserver, Disposable {
    val upstream: AtomicReference<Disposable> = AtomicReference()

    override fun onSubscribe(d: Disposable) {
        if (EndConsumerHelper.setOnce(this.upstream, d, javaClass)) {
            onStart()
        }
    }

    /**
     * Called once the single upstream [Disposable] is set via [.onSubscribe].
     */
    protected abstract fun onStart()

    protected abstract fun onDispose()

    override fun isDisposed(): Boolean {
        return upstream.get() === DisposableHelper.DISPOSED
    }

    @CallSuper
    override fun dispose() {
        if (DisposableHelper.dispose(upstream)) {
            onDispose()
        }
    }
}
