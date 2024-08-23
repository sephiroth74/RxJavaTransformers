package it.sephiroth.android.rxjava3.extensions

import android.os.Looper
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import it.sephiroth.android.rxjava3.extensions.completable.autoSubscribe
import it.sephiroth.android.rxjava3.extensions.completable.debug
import it.sephiroth.android.rxjava3.extensions.completable.debugWithThread
import it.sephiroth.android.rxjava3.extensions.completable.delay
import it.sephiroth.android.rxjava3.extensions.completable.observeMain
import it.sephiroth.android.rxjava3.extensions.completable.retryWhen
import it.sephiroth.android.rxjava3.extensions.observers.AutoDisposableCompletableObserver
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong


/**
 * RxJavaExtensions
 * $ adb shell am instrument -w -m  --no-window-animation  -e debug false -e class 'it.sephiroth.android.rxjava2.extensions.CompletableAndroidTest' it.sephiroth.android.rxjava2.extensions.test/androidx.test.runner.AndroidJUnitRunner
 *
 * @author Alessandro Crugnola on 02.03.21 - 13:26
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class CompletableAndroidTest {

    @Test
    fun test001() {
        val latch = CountDownLatch(1)
        val now = SystemClock.elapsedRealtime()
        delay(100, TimeUnit.MILLISECONDS) {
            val elapsed = SystemClock.elapsedRealtime() - now
            Assert.assertTrue(elapsed > 100)
            latch.countDown()
        }
        latch.await()
    }

    @Test
    fun test002() {
        val mainThread = Looper.getMainLooper().thread
        val latch = CountDownLatch(1)
        Completable.complete()
            .subscribeOn(Schedulers.computation())
            .observeMain()
            .autoSubscribe {
                doOnComplete {
                    Assert.assertEquals(mainThread, Thread.currentThread())
                    latch.countDown()
                }
            }
        latch.await()
    }

    @Test
    fun test003() {
        val result = mutableListOf<String>()
        val latch = CountDownLatch(1)
        Completable.complete().autoSubscribe {
            doOnStart {
                result.add("start")
            }
            doOnComplete {
                result.add("complete")
                latch.countDown()
            }
            doOnError {
                result.add("error")
            }
        }

        latch.await()
        Assert.assertEquals(listOf("start", "complete"), result)
    }

    @Test
    fun test004() {
        val result = mutableListOf<String>()
        val latch = CountDownLatch(1)
        Completable.error(RuntimeException("test exception")).autoSubscribe {
            doOnStart {
                result.add("start")
            }
            doOnComplete {
                result.add("complete")
            }
            doOnError {
                result.add("error")
                latch.countDown()
            }
        }

        latch.await()
        Assert.assertEquals(listOf("start", "error"), result)
    }

    @Test
    fun test005() {
        val latch = CountDownLatch(1)
        val disposable = Completable.complete().autoSubscribe()
        delay(16, TimeUnit.MILLISECONDS) {
            Assert.assertTrue(disposable.isDisposed)
            latch.countDown()
        }
        latch.await()
    }

    @Test
    fun test006() {
        val result = mutableListOf<String>()
        val latch = CountDownLatch(1)
        Completable.complete().autoSubscribe(AutoDisposableCompletableObserver {
            doOnStart { result.add("start") }
            doOnComplete { result.add("complete"); latch.countDown() }
            doOnError { result.add("error") }
        })

        latch.await()
        Assert.assertEquals(listOf("start", "complete"), result)
    }

    @Test
    fun test007() {
        val result = mutableListOf<String>()
        val latch = CountDownLatch(1)
        Completable.complete().autoSubscribe(AutoDisposableCompletableObserver {
            doOnStart { result.add("start") }
            doOnComplete { result.add("complete"); latch.countDown() }
            doOnError { result.add("error") }
        })

        latch.await()
        Assert.assertEquals(listOf("start", "complete"), result)
    }

    @Test
    fun test008() {
        val result = mutableListOf<String>()
        val latch = CountDownLatch(1)
        Completable.error(RuntimeException("test exception"))
            .autoSubscribe(AutoDisposableCompletableObserver {
                doOnStart { result.add("start") }
                doOnComplete { result.add("complete") }
                doOnError { result.add("error"); latch.countDown() }
            })

        latch.await()
        Assert.assertEquals(listOf("start", "error"), result)
    }


    @Test
    fun test009() {
        Completable.complete().debug("c1").debugWithThread("c1-tread").test().await()
            .assertComplete()
        Completable.error(RuntimeException("test")).debug("c2").debugWithThread("c2-tread").test()
            .await().assertError(RuntimeException::class.java)

        val latch = CountDownLatch(1)
        val result = CountDownLatch(1)

        val disposable = Completable.complete()
            .delaySubscription(1, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.computation())
            .debug("c3")
            .debugWithThread("c3-thread")
            .subscribe()

        delay(16, TimeUnit.MILLISECONDS) {
            println("ok. go.")
            disposable.dispose()
            latch.countDown()
        }

        latch.await()
    }

    @Test
    fun test016() {
        val now = System.currentTimeMillis()
        val finalTime = AtomicLong()
        val maxAttempts = 5

        Completable.create { emitter ->
            if (!emitter.isDisposed) {
                val delta = System.currentTimeMillis() - now
                println("[${Date()}] emitting the exception. delta = $delta")
                if (delta >= TimeUnit.SECONDS.toMillis(maxAttempts.toLong() / 2)) {
                    emitter.onComplete()
                } else {
                    emitter.tryOnError(IllegalStateException("Error emitting [$delta]"))
                }
            }
        }.retryWhen(maxAttempts) { _, retryCount ->
            println("[${Date()}] predicate[$retryCount - $maxAttempts]")
            TimeUnit.SECONDS.toMillis(1) // retry every 1 second
        }.subscribeOn(Schedulers.single())
            .doOnComplete {
                println("[${Date()}] onComplete")
                finalTime.set(System.currentTimeMillis() - now)
            }.doOnError {
                println("[${Date()}] onError(${it.javaClass})")
            }
            .test()
            .await()
            .assertComplete()

        Assert.assertTrue(
            "final time must be >= ${maxAttempts / 2} seconds but it was ${finalTime.get()}",
            finalTime.get() >= TimeUnit.SECONDS.toMillis(maxAttempts.toLong() / 2)
        )
        Assert.assertTrue(
            "final time must be < ${(maxAttempts / 2) + 1} seconds but it was ${finalTime.get()}",
            finalTime.get() < TimeUnit.SECONDS.toMillis(maxAttempts.toLong() / 2 + 1)
        )
    }


    @Test
    fun test017() {
        val now = System.currentTimeMillis()
        val finalTime = AtomicLong()
        val maxAttempts = 6

        Completable.create { emitter ->
            if (!emitter.isDisposed) {
                println("[${Date()}] emitting the exception")
                emitter.tryOnError(IllegalStateException("Error"))
            }
        }.retryWhen(maxAttempts) { throwable, retryCount ->
            println("[${Date()}] predicate[$retryCount -- $maxAttempts] (throwable: ${throwable.javaClass})")
            (retryCount * 50).toLong()
        }.subscribeOn(Schedulers.single())
            .doOnComplete {
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

    @Test
    fun test018() {
        val latch = CountDownLatch(3)

        delay(0, TimeUnit.MILLISECONDS, Schedulers.io()) {
            println("delay {0} completed on thread ${Thread.currentThread().name}")
            Assert.assertEquals(Thread.currentThread(), ioThread)
            latch.countDown()
        }

        delay(100, TimeUnit.MILLISECONDS, Schedulers.single()) {
            println("delay {100} completed on thread ${Thread.currentThread().name}")
            Assert.assertEquals(Thread.currentThread(), singleThread)
            latch.countDown()
        }

        delay(200, TimeUnit.MILLISECONDS) {
            println("delay {200} completed on thread ${Thread.currentThread().name}")
            Assert.assertEquals(Thread.currentThread(), Looper.getMainLooper().thread)
            latch.countDown()
        }

        latch.await()
    }

    @Test
    fun test019() {
        val latch = CountDownLatch(1)
        var disposable: AutoDisposableCompletableObserver? =
            Completable.complete().delay(100, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.single())
                .autoSubscribe {
                    doOnStart { println("doOnStart") }

                    doOnComplete {
                        println("doOnComplete")
                        latch.countDown()
                    }

                    doOnError { println("doOnError") }

                    doOnDispose { println("doOnDispose") }
                }

        latch.await(1, TimeUnit.SECONDS)
        Assert.assertEquals(0, latch.count)
        System.gc()
        disposable = null
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
}
