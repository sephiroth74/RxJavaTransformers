package it.sephiroth.android.rxjava3.extensions

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableOperator
import io.reactivex.rxjava3.core.FlowableSubscriber
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.exceptions.Exceptions
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.internal.fuseable.HasUpstreamPublisher
import io.reactivex.rxjava3.internal.operators.flowable.FlowableInternalHelper
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper
import io.reactivex.rxjava3.observers.LambdaConsumerIntrospection
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.processors.PublishProcessor
import io.reactivex.rxjava3.schedulers.Schedulers
import it.sephiroth.android.rxjava3.extensions.flowable.autoSubscribe
import it.sephiroth.android.rxjava3.extensions.flowable.observeMain
import it.sephiroth.android.rxjava3.extensions.operators.FlowableTransformers
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 01.03.21 - 10:53
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class FlowableTransformerAndroidTest {

    @Test
    fun test01() {
        val now = System.currentTimeMillis()

        val o1 = PublishProcessor.create<Boolean>({ emitter ->
            Thread.sleep(550)
            emitter.onNext(true)
            emitter.onNext(false)

            Thread.sleep(550)
            emitter.onNext(true)
            emitter.onNext(false)

            Thread.sleep(550)
            emitter.onNext(true)
            emitter.onNext(false)

            emitter.onComplete()
        }, BackpressureStrategy.BUFFER).subscribeOn(Schedulers.newThread())


        Flowable
            .create<Long>({ emitter ->
                emitter.onNext(1)
                emitter.onNext(2)

                Thread.sleep(500)
                emitter.onNext(3)

                Thread.sleep(500)
                emitter.onNext(4)
                emitter.onNext(5)
                emitter.onNext(6)

                Thread.sleep(500)
                emitter.onNext(7)
                emitter.onNext(8)

                emitter.onComplete()
            }, BackpressureStrategy.BUFFER)
            .subscribeOn(Schedulers.newThread())
            .compose(FlowableTransformers.valveLast(o1, false))
            .doOnNext {
                println("Target: [${System.currentTimeMillis() - now}] received onNext($it)")
            }
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertValues(3, 6, 8)
            .assertValueCount(3)

        println("done")


    }

    @Test
    fun test02() {
        val o1 = Flowable.just(1, 2, 3, 4, 5)
        val o2 = PublishProcessor.create<Boolean>()
        o1.compose(FlowableTransformers.valveLast(o2)).test().await().assertComplete()
            .assertValues(1, 2, 3, 4, 5)
    }

    @Test
    fun test03() {
        var i = 0
        val latch = CountDownLatch(14)
        val disposable = Flowable.create<Int>({ emitter ->
            while (i < 10) {
                if (emitter.isCancelled) return@create
                emitter.onNext(i)
                i++
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    if (!emitter.isCancelled) emitter.onError(e)
                    return@create
                }
            }
            emitter.onComplete()
        }, BackpressureStrategy.BUFFER)
            .subscribeOn(Schedulers.single())
            .observeMain()
            .autoSubscribe {
                doOnStart {
                    println("doOnStart")
                    latch.countDown()
                }
                doOnDispose {
                    println("doOnDispose")
                    latch.countDown()
                }
                doOnError {
                    println("doOnError")
                }
                doOnFinish {
                    println("doOnFinish")
                    latch.countDown()
                }
                doOnComplete {
                    println("doOnComplete")
                    latch.countDown()
                }
                doOnNext {
                    println("doOnNext($it)")
                    latch.countDown()
                }
            }

        println("now waiting...")
        latch.await(5, TimeUnit.SECONDS)
        Thread.sleep(1000)

        Assert.assertTrue(disposable.isDisposed)

        println("done")
    }

    companion object {
        private lateinit var ioThread: Thread
        private lateinit var singleThread: Thread

        @JvmStatic
        @BeforeClass
        fun before() {
            val latch = CountDownLatch(2)

            Schedulers.single().scheduleDirect {
                singleThread = Thread.currentThread()
                latch.countDown()
            }

            Schedulers.io().scheduleDirect {
                ioThread = Thread.currentThread()
                latch.countDown()
            }
            latch.await()

        }
    }

    private fun <T : Event> subscribe(upstream: Flowable<T>, action: (T) -> Unit): Disposable {
        val ls = LambdaSubscriber(action, FlowableInternalHelper.RequestMax.INSTANCE)

        if (upstream is HasUpstreamPublisher<*>) {
            println("upstream.source=${upstream.source()}")
        }

        upstream.subscribe(ls)
        return ls
    }

    abstract class Event
    class Event01 : Event()
    class Event02 : Event()
}


class LambdaSubscriber<T : Any>(
    private val action: (T) -> Unit,
    private val onSubscribe: Consumer<Subscription>
) : AtomicReference<Subscription?>(), FlowableSubscriber<T>, Subscription, Disposable,
    LambdaConsumerIntrospection {

    override fun onSubscribe(s: Subscription) {
        println("LambdaSubscriber::onSubscribe(${s.javaClass}, ${System.identityHashCode(s)})")
        if (SubscriptionHelper.setOnce(this, s)) {
            onSubscribe.accept(this)
        }
    }

    override fun onNext(t: T) {
        println("LambdaSubscriber::onNext()")
        if (!isDisposed) {
            try {
                action.invoke(t)
            } catch (e: Throwable) {
                Exceptions.throwIfFatal(e)
                get()?.cancel()
                onError(e)
            }
        }
    }

    override fun onError(t: Throwable) {
        RxJavaPlugins.onError(t)
    }

    override fun onComplete() {
        println("LambdaSubscriber::onComplete()")
    }

    override fun dispose() {
        println("LambdaSubscriber::dispose()")
        cancel()
    }

    override fun isDisposed(): Boolean {
        return get() === SubscriptionHelper.CANCELLED
    }

    override fun request(n: Long) {
        get()?.request(n)
    }

    override fun cancel() {
        println("LambdaSubscriber::cancel()")
        SubscriptionHelper.cancel(this)
    }

    override fun hasCustomOnError(): Boolean {
        return false
    }

    override fun toString(): String {
        return "LambdaSubscriber(${System.identityHashCode(this)})"
    }


}


internal class CustomOperator<T : Any> : FlowableOperator<T, T> {
    override fun apply(upstream: Subscriber<in T>): Subscriber<in T> {
        return CustomSubscriber<T>(upstream)
    }
}

class CustomSubscriber<T : Any>(private val downstream: Subscriber<in T>) : FlowableSubscriber<T>,
    Subscription {
    private val tag = "CustomSubscriber[${System.identityHashCode(this)}]"

    private var upstream: Subscription? = null

    override fun onSubscribe(s: Subscription) {
        println("$tag: onSubscribe(upstream=$upstream, s=${s.javaClass})")
        println("downstream=$downstream")
        if (upstream != null) {
            s.cancel()
        } else {
            upstream = s
            downstream.onSubscribe(this)
        }
    }

    override fun onNext(item: T) {
        println("$tag: onNext($item)")
        downstream.onNext(item)
//        upstream!!.request(1)
    }

    override fun onError(throwable: Throwable) {
        println("$tag: onError()")
        downstream.onError(throwable)
    }

    override fun onComplete() {
        println("$tag: onComplete()")
        downstream.onComplete()
    }

    override fun request(n: Long) {
        upstream!!.request(n)
    }

    override fun cancel() {
        println("$tag: cancel()")
        upstream!!.cancel()
    }
}
