package it.sephiroth.android.rxjava3.extensions.operators

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.functions.Function
import io.reactivex.rxjava3.internal.observers.BasicFuseableObserver


/**
 * @author Alessandro Crugnola on 18.12.21 - 21:03
 */
class ObservableMapNotNull<T : Any, R : Any>(
    private val source: Observable<T>,
    private val mapper: Function<in T, out R?>
) : Observable<R>() {

    override fun subscribeActual(observer: Observer<in R>) {
        source.subscribe(MapNotNullObserver(observer, mapper))
    }

    internal class MapNotNullObserver<T, R>(
        downstream: Observer<in R>,
        private val mapper: Function<in T, out R?>
    ) :
        BasicFuseableObserver<T, R>(downstream) where T : Any, R : Any? {
        override fun onNext(t: T) {
            if (done) {
                return
            }

            if (sourceMode != NONE) {
                downstream.onNext(null)
                return
            }
            val result: R? = try {
                mapper.apply(t)
            } catch (ex: Throwable) {
                fail(ex)
                return
            }
            if (null != result) {
                downstream.onNext(result)
            }
        }

        override fun requestFusion(mode: Int): Int {
            return transitiveBoundaryFusion(mode)
        }

        @Throws(Throwable::class)
        override fun poll(): R? {
            while (true) {
                val item = qd.poll() ?: return null
                val result = mapper.apply(item)
                if (null != result) {
                    return result
                }
            }
        }
    }
}
