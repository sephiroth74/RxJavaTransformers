package it.sephiroth.android.rxjava3.extensions.operators

import io.reactivex.rxjava3.core.FlowableOperator
import io.reactivex.rxjava3.functions.Function
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

class FlowableOperatorDoOnNth<T : Any>(val action: Function<T, Unit>, val nth: Long) :
    FlowableOperator<T, T> {
    private var count = 0L

    override fun apply(subscriber: Subscriber<in T>): Subscriber<in T> {
        return object : Subscriber<T> {
            override fun onSubscribe(s: Subscription?) {
                subscriber.onSubscribe(s)
            }

            override fun onNext(t: T) {
                if (count++ == nth) {
                    action.apply(t)
                }
                subscriber.onNext(t)
            }

            override fun onError(e: Throwable) {
                subscriber.onError(e)
            }

            override fun onComplete() {
                subscriber.onComplete()
            }
        }
    }
}
