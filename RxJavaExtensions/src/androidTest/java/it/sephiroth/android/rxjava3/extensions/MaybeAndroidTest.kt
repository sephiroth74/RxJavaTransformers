package it.sephiroth.android.rxjava3.extensions

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.schedulers.Schedulers
import it.sephiroth.android.rxjava3.extensions.maybe.*
import it.sephiroth.android.rxjava3.extensions.observers.AutoDisposableMaybeObserver
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 02.03.21 - 16:50
 */
@RunWith(AndroidJUnit4::class)
class MaybeAndroidTest {

    @Test
    fun test001() {
        val mainThread = Looper.getMainLooper().thread
        val currentThread = Thread.currentThread()

        val latch = CountDownLatch(1)
        Maybe.just(1)
            .subscribeOn(Schedulers.single())
            .observeMain()
            .autoSubscribe {
                doOnStart {
                    Assert.assertEquals(currentThread, Thread.currentThread())
                }

                doOnFinish {
                    Assert.assertEquals(mainThread, Thread.currentThread())
                    latch.countDown()
                }

                doOnComplete {
                    Assert.assertEquals(mainThread, Thread.currentThread())
                }

                doOnError {
                    Assert.assertEquals(mainThread, Thread.currentThread())
                }

                doOnSuccess {
                    Assert.assertEquals(mainThread, Thread.currentThread())
                }
            }
        latch.await()
    }


    @Test
    fun test002() {
        val result = mutableListOf<String>()
        val latch = CountDownLatch(1)
        Maybe.just(1).autoSubscribe {
            doOnStart { result.add("start") }
            doOnError { result.add("error") }
            doOnSuccess { result.add("success:$it") }
            doOnComplete { result.add("complete") }
            doOnFinish { result.add("finish"); latch.countDown() }
        }

        latch.await()
        Assert.assertEquals(listOf("start", "success:1", "finish"), result)
    }

    @Test
    fun test003() {
        val result = mutableListOf<String>()
        val latch = CountDownLatch(1)
        Maybe.empty<Int>().autoSubscribe {
            doOnStart { result.add("start") }
            doOnError { result.add("error") }
            doOnSuccess { result.add("success:$it") }
            doOnComplete { result.add("complete") }
            doOnFinish { result.add("finish"); latch.countDown() }
        }

        latch.await()
        Assert.assertEquals(listOf("start", "complete", "finish"), result)
    }

    @Test
    fun test004() {
        val result = mutableListOf<String>()
        val latch = CountDownLatch(1)
        Maybe.error<Int>(RuntimeException("test")).autoSubscribe {
            doOnStart { result.add("start") }
            doOnError { result.add("error") }
            doOnSuccess { result.add("success:$it") }
            doOnComplete { result.add("complete") }
            doOnFinish { result.add("finish"); latch.countDown() }
        }

        latch.await()
        Assert.assertEquals(listOf("start", "error", "finish"), result)
    }

    @Test
    fun test005() {
        val result = mutableListOf<String>()
        val latch = CountDownLatch(1)
        Maybe.just(1).autoSubscribe(AutoDisposableMaybeObserver {
            doOnStart { result.add("start") }
            doOnError { result.add("error") }
            doOnSuccess { result.add("success:$it") }
            doOnComplete { result.add("complete") }
            doOnFinish { result.add("finish"); latch.countDown() }
        })

        latch.await()
        Assert.assertEquals(listOf("start", "success:1", "finish"), result)
    }

    @Test
    fun test006() {
        var latch = CountDownLatch(1)
        var disposable = Maybe.just(1).debug("m1").debugWithThread("m1-thread").doOnSuccess { latch.countDown() }.autoSubscribe()
        latch.await()
        Assert.assertTrue(disposable.isDisposed)

        latch = CountDownLatch(1)
        disposable = Maybe.empty<Int>().debug("m2").debugWithThread("m2-thread").doOnComplete { latch.countDown() }.autoSubscribe()
        latch.await()
        Assert.assertTrue(disposable.isDisposed)

        latch = CountDownLatch(1)
        disposable = Maybe.error<Int>(RuntimeException()).debug("m3").debugWithThread("m3-thread").doOnError { latch.countDown() }.autoSubscribe()
        latch.await()
        Assert.assertTrue(disposable.isDisposed)
    }

    @Test
    fun test007() {
        val now = System.currentTimeMillis()
        val finalTime = AtomicLong()
        val maxAttempts = 5

        Maybe.create<String> { emitter ->
            if (!emitter.isDisposed) {
                val delta = System.currentTimeMillis() - now
                println("[${Date()}] emitting the exception. delta = $delta")
                if (delta >= TimeUnit.SECONDS.toMillis(maxAttempts.toLong() / 2)) {
                    emitter.onSuccess("Emitting next Value!")
                } else {
                    emitter.tryOnError(IllegalStateException("Error emitting [$delta]"))
                }
            }
        }.retryWhen(maxAttempts) { _, retryCount ->
            println("[${Date()}] predicate[$retryCount - $maxAttempts]")
            TimeUnit.SECONDS.toMillis(1) // retry every 1 second
        }.subscribeOn(Schedulers.single())
            .doOnSuccess {
                println("[${Date()}] onComplete")
                finalTime.set(System.currentTimeMillis() - now)
            }.doOnError {
                println("[${Date()}] onError(${it.javaClass})")
            }
            .test()
            .await()
            .assertComplete()

        Assert.assertTrue("final time must be >= ${maxAttempts / 2} seconds but it was ${finalTime.get()}", finalTime.get() >= TimeUnit.SECONDS.toMillis(maxAttempts.toLong() / 2))
        Assert.assertTrue("final time must be < ${(maxAttempts / 2) + 1} seconds but it was ${finalTime.get()}", finalTime.get() < TimeUnit.SECONDS.toMillis(maxAttempts.toLong() / 2 + 1))
    }


    @Test
    fun test008() {
        val now = System.currentTimeMillis()
        val finalTime = AtomicLong()
        val maxAttempts = 10

        Maybe.create<String> { emitter ->
            if (!emitter.isDisposed) {
                println("[${Date()}] emitting the exception")
                emitter.tryOnError(IllegalStateException("Error"))
            }
        }.retryWhen(maxAttempts) { throwable, retryCount ->
            println("[${Date()}] predicate[$retryCount -- $maxAttempts] (throwable: ${throwable.javaClass})")
            (retryCount * 100).toLong()
        }.subscribeOn(Schedulers.single())
            .doOnSuccess {
                println("[${Date()}] onComplete")
                finalTime.set(System.currentTimeMillis() - now)
            }.doOnError {
                println("[${Date()}] onError")
                finalTime.set(System.currentTimeMillis() - now)
            }
            .test()
            .await()
            .assertError(RetryException::class.java)
    }

    companion object {
        private lateinit var ioThread: Thread
        private lateinit var singleThread: Thread

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
        }
    }
}

