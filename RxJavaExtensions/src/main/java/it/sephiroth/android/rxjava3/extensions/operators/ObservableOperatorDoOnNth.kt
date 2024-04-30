package it.sephiroth.android.rxjava3.extensions.operators

import io.reactivex.rxjava3.core.ObservableOperator
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.functions.Function

class ObservableOperatorDoOnNth<T : Any>(val action: Function<T, Unit>, val nth: Long) :
    ObservableOperator<T, T> {
    private var count = 0L
    override fun apply(observer: Observer<in T>): Observer<in T> {
        return object : Observer<T> {
            override fun onSubscribe(d: io.reactivex.rxjava3.disposables.Disposable) {
                observer.onSubscribe(d)
            }

            override fun onNext(t: T) {
                if (count++ == nth) {
                    action.apply(t)
                }
                observer.onNext(t)
            }

            override fun onError(e: Throwable) {
                observer.onError(e)
            }

            override fun onComplete() {
                observer.onComplete()
            }
        }
    }
}
