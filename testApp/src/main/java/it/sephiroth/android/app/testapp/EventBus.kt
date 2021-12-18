package it.sephiroth.android.app.testapp

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.processors.FlowableProcessor
import io.reactivex.rxjava3.processors.PublishProcessor
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber


/**
 * Base Event Bus class implementation.
 * Events dispatch/delivery are guaranteed to be sent in a serialized queue.
 *
 * @author Alessandro Crugnola on 11/20/20 - 9:08 AM
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
open class EventBus<T>(protected val name: String) where T : EventBus.Event {

    val defaultPriority = 5

    private val logger: Timber.Tree = Timber.tag(name)

    // default observer scheduler
    protected val dispatcherScheduler: Scheduler = AndroidSchedulers.mainThread()

    protected val subscriberScheduler: Scheduler = Schedulers.computation()

    // original publisher
    protected val publisher: FlowableProcessor<T> = PublishProcessor.create<T>().toSerialized()

    // flowable where all subscription will be added
    private val flowable: Flowable<T> = publisher
        .share()
        .retry()
        .onBackpressureBuffer()
        .subscribeOn(subscriberScheduler)
        .observeOn(dispatcherScheduler)

    // ----------------------------------------------------
    // region Inlined Methods

    inline fun <reified E> listen(): Flowable<E> where E : T = listen(E::class.java)

    inline fun <reified E> listen(crossinline func: (E) -> Unit): Disposable where E : T = listen(E::class.java).subscribe { func.invoke(it) }

    // endregion Inlined Methods

    // ----------------------------------------------------
    // region Public Methods

    fun <E> listen(eventType: Class<E>): Flowable<E> where E : T = observe(eventType)

    fun <E> listen(eventType: Class<E>, consumer: Consumer<T>): Disposable where E : T = listen(eventType).subscribe(consumer)

    fun observe(): Flowable<T> {
        logger.i("observe")
        return flowable
    }

    // endregion Public Methods

    private fun <E> observe(eventClass: Class<E>): Flowable<E> where E : T {
        logger.i("observe(${eventClass.name})")
        return flowable.ofType(eventClass)
    }

    @Synchronized
    fun send(event: T) {
        logger.v("(event) ---> ${event::class.java.name}")
        publisher.onNext(event)
    }

    interface Event
}

