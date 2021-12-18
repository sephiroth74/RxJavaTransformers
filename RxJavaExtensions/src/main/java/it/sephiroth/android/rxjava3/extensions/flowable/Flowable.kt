@file:Suppress("unused")

package it.sephiroth.android.rxjava3.extensions.flowable


import android.util.Log
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import it.sephiroth.android.rxjava3.extensions.observable.autoSubscribe
import it.sephiroth.android.rxjava3.extensions.observers.AutoDisposableObserver
import it.sephiroth.android.rxjava3.extensions.observers.AutoDisposableSubscriber
import java.util.concurrent.TimeUnit

/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 06.01.21 - 13:31
 */


/**
 * Subscribe the source using an instance of the [AutoDisposableObserver].
 * The source will be disposed when a complete or error event is received.
 */
fun <T> Flowable<T>.autoSubscribe(observer: AutoDisposableSubscriber<T>): AutoDisposableSubscriber<T> where T : Any {
    return this.subscribeWith(observer)
}

/**
 * @see [autoSubscribe]
 */
fun <T> Flowable<T>.autoSubscribe(builder: (AutoDisposableSubscriber<T>.() -> Unit)): AutoDisposableSubscriber<T> where T : Any {
    return this.subscribeWith(AutoDisposableSubscriber(builder))
}


fun <T> Flowable<T>.debug(tag: String): Flowable<T> where T : Any {
    return this
        .doOnNext { Log.v(tag, "onNext($it)") }
        .doOnError { Log.e(tag, "onError(${it.message})") }
        .doOnSubscribe { Log.v(tag, "onSubscribe()") }
        .doOnComplete { Log.v(tag, "onComplete()") }
        .doOnCancel { Log.w(tag, "onCancel()") }
        .doOnRequest { Log.w(tag, "onRequest()") }
        .doOnTerminate { Log.w(tag, "onTerminate()") }
}

fun <T> Flowable<T>.debugWithThread(tag: String): Flowable<T> where T : Any {
    return this
        .doOnNext { Log.v(tag, "[${Thread.currentThread().name}] onNext($it)") }
        .doOnError { Log.e(tag, "[${Thread.currentThread().name}] onError(${it.message})") }
        .doOnSubscribe { Log.v(tag, "[${Thread.currentThread().name}] onSubscribe()") }
        .doOnComplete { Log.v(tag, "[${Thread.currentThread().name}] onComplete()") }
        .doOnCancel { Log.w(tag, "[${Thread.currentThread().name}] onCancel()") }
        .doOnRequest { Log.w(tag, "[${Thread.currentThread().name}] onRequest()") }
        .doOnTerminate { Log.w(tag, "[${Thread.currentThread().name}] onTerminate()") }
}

/**
 * alias for Flowable.observeOn(AndroidSchedulers.mainThread())
 */
fun <T> Flowable<T>.observeMain(): Flowable<T> where T : Any {
    return observeOn(AndroidSchedulers.mainThread())
}

/**
 * Returns a Flowable that skips all items emitted by the source emitter
 * until a specified interval passed between each emission..
 *
 * @param time minimum amount of time need to pass between each interactions
 * @param unit time unit for the [time] param
 * @param defaultOpened if true the very first emission of the source [Flowable] will be allowed, false otherwise
 *
 */
@Deprecated("use debounce instead")
fun <T : Any> Flowable<T>.skipBetween(
    time: Long,
    unit: TimeUnit,
    defaultOpened: Boolean = true
): Flowable<T> {
    var t: Long = if (defaultOpened) 0 else System.currentTimeMillis()
    return this.filter {
        val waitTime = unit.toMillis(time)
        val elapsed = (System.currentTimeMillis() - t)
        if (elapsed > waitTime) {
            t = System.currentTimeMillis()
            true
        } else {
            false
        }
    }
}

/**
 * Returns a Flowable that filter out those objects not of the type of [cls1] and [cls2].
 * Moreover objects must alternate between cls1 and cls2, otherwise the object is skipped.
 *
 * For instance this code:
 *
 *             subject.retry()
 *             .toFlowable(BackpressureStrategy.BUFFER)
 *             .pingPong(TestEventImpl2::class.java, TestEventImpl4::class.java)
 *               .doOnNext { it ->
 *                   Log.v("FlowableTest", "onNext = $it")
 *               }
 *               .subscribe()
 *               subject.onNext(TestEventImpl1())
 *               subject.onNext(TestEventImpl2())
 *               subject.onNext(TestEventImpl2())
 *               subject.onNext(TestEventImpl3())
 *               subject.onNext(TestEventImpl4())
 *               subject.onNext(TestEventImpl4())
 *               subject.onNext(TestEventImpl1())
 *               subject.onNext(TestEventImpl2())
 *               subject.onNext(TestEventImpl3())
 *               subject.onNext(TestEventImpl4())
 *
 * It will only output the following:
 *
 *      FlowableTest: onNext = TestEventImpl2
 *      FlowableTest: onNext = TestEventImpl4
 *      FlowableTest: onNext = TestEventImpl2
 *      FlowableTest: onNext = TestEventImpl4
 */
fun <T, E, R> Flowable<T>.pingPong(cls1: Class<E>, cls2: Class<R>): Flowable<T> where E : T, R : T, T : Any {
    var current: Class<*>? = null
    return this.doOnSubscribe {
        current = null
    }.filter { t2: T ->
        if (cls1.isInstance(t2) || cls2.isInstance(t2)) {
            val t2Class = t2::class.java
            val result = if (t2Class != cls1 && t2Class != cls2) {
                true
            } else {
                if (null == current) {
                    if (t2Class == cls2 || t2Class == cls1) {
                        current = t2Class
                        true
                    } else {
                        false
                    }
                } else {
                    if (current == t2Class) {
                        false
                    } else {
                        current = t2Class
                        true
                    }
                }
            }
            result
        } else {
            false
        }
    }
}
