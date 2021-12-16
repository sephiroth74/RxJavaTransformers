package it.sephiroth.android.rxjava3.extensions.flowable

import android.util.Log
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.processors.FlowableProcessor
import io.reactivex.rxjava3.processors.PublishProcessor
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription


/**
 * RxJavaExtensions
 * Flowable which accept priority subscriptions
 *
 * @author Alessandro Crugnola on 15.12.21 - 09:08
 */
private class PrioritizedFlowable<T>(s: Flowable<T>) : Flowable<T>(), Subscriber<T> where T : Any {

    private val source = s.publish().refCount()
    private val intermediary = PublishProcessor.create<T>().toSerialized()
    private val router = arrayListOf<Pair<Int, FlowableProcessor<T>>>()

    init {
        source.subscribe(intermediary)
    }

    override fun subscribeActual(subscriber: Subscriber<in T>) {
        prioritySubscribe(5, subscriber)
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
    fun prioritySubscribe(subscriber: Subscriber<in T>) = prioritySubscribe(5, subscriber)

    @Suppress("unused")
    fun prioritySubscribe(consumer: Consumer<in T>) = prioritySubscribe(5, consumer)

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

