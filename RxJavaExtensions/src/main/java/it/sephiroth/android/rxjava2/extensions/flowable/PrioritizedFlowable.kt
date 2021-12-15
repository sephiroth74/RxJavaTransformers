package it.sephiroth.android.rxjava2.extensions.flowable

import android.util.Log
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.processors.FlowableProcessor
import io.reactivex.processors.PublishProcessor
import it.sephiroth.android.rxjava2.extensions.observable.PrioritizedObservable
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription


/**
 * RxJavaExtensions
 * Flowable which accept priority subscriptions
 *
 * @author Alessandro Crugnola on 15.12.21 - 09:08
 */
class PrioritizedFlowable<T>(s: Flowable<T>) : Flowable<T>(), Subscriber<T> {

    private val source = s.publish().refCount()
    private val intermediary = PublishProcessor.create<T>().toSerialized()
    private val router = arrayListOf<Pair<Int, FlowableProcessor<T>>>()

    init {
        source.subscribe(intermediary)
    }

    override fun subscribeActual(subscriber: Subscriber<in T>) {
        prioritySubscribe(PrioritizedObservable.PRIORITY_DEFAULT.value, subscriber)
    }

    override fun onSubscribe(s: Subscription) {
        router.forEach { it.second.onSubscribe(s) }
    }

    override fun onNext(t: T) {
        Log.i(javaClass.simpleName, "onNext($t)")
        router.forEach { it.second.onNext(t) }
    }

    override fun onError(e: Throwable) {
        router.forEach { it.second.onError(e) }
    }

    override fun onComplete() {
        router.forEach { it.second.onComplete() }
    }

    @Suppress("unused")
    fun prioritySubscribe(subscriber: Subscriber<in T>) = prioritySubscribe(PrioritizedObservable.PRIORITY_DEFAULT.value, subscriber)

    @Suppress("unused")
    fun prioritySubscribe(consumer: Consumer<in T>) = prioritySubscribe(PrioritizedObservable.PRIORITY_DEFAULT.value, consumer)

    @Suppress("MemberVisibilityCanBePrivate")
    fun prioritySubscribe(priority: Int, subscriber: Subscriber<in T>): Disposable {
        val subject = PublishProcessor.create<T>()
        val disposable = subject.subscribe({ subscriber.onNext(it) }, { subscriber.onError(it) }, { subscriber.onComplete() })

        val entry = Pair(priority, subject)
        router.add(entry)
        router.sortBy { it.first }

//        intermediary.subscribe(subscriber)

        return object : Disposable {
            override fun dispose() {
                disposable.dispose()
                router.remove(entry)
            }

            override fun isDisposed(): Boolean {
                return disposable.isDisposed
            }
        }
    }

    fun prioritySubscribe(priority: Int, consumer: Consumer<in T>): Disposable {
        val subject = PublishProcessor.create<T>()
        val disposable = subject.subscribe(consumer)

        val entry = Pair(priority, subject)
        router.add(entry)
        router.sortBy { it.first }

//        intermediary.subscribe(subject)

        return object : Disposable {
            override fun dispose() {
                disposable.dispose()
                router.remove(entry)
            }

            override fun isDisposed(): Boolean {
                return disposable.isDisposed
            }
        }
    }
}

