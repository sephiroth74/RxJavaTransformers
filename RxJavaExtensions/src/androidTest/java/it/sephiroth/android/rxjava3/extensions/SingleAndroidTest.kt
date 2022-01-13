package it.sephiroth.android.rxjava3.extensions

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import it.sephiroth.android.rxjava3.extensions.observers.AutoDisposableSingleObserver
import it.sephiroth.android.rxjava3.extensions.single.*
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
class SingleAndroidTest {

    @Test
    fun test001() {
        val mainThread = Looper.getMainLooper().thread
        val currentThread = Thread.currentThread()

        val latch = CountDownLatch(1)
        Single.just(1)
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
        Single.just(1).autoSubscribe {
            doOnStart { result.add("start") }
            doOnError { result.add("error") }
            doOnSuccess { result.add("success:$it") }
            doOnFinish { result.add("finish"); latch.countDown() }
        }

        latch.await()
        Assert.assertEquals(listOf("start", "success:1", "finish"), result)
    }

    @Test
    fun test004() {
        val result = mutableListOf<String>()
        val latch = CountDownLatch(1)
        Single.error<Int>(RuntimeException("test")).autoSubscribe {
            doOnStart { result.add("start") }
            doOnError { result.add("error") }
            doOnSuccess { result.add("success:$it") }
            doOnFinish { result.add("finish"); latch.countDown() }
        }

        latch.await()
        Assert.assertEquals(listOf("start", "error", "finish"), result)
    }

    @Test
    fun test005() {
        val result = mutableListOf<String>()
        val latch = CountDownLatch(1)
        Single.just(1).autoSubscribe(AutoDisposableSingleObserver {
            doOnStart { result.add("start") }
            doOnError { result.add("error") }
            doOnSuccess { result.add("success:$it") }
            doOnFinish { result.add("finish"); latch.countDown() }
        })

        latch.await()
        Assert.assertEquals(listOf("start", "success:1", "finish"), result)
    }

    @Test
    fun test006() {
        var latch = CountDownLatch(1)
        var disposable = Single.just(1).debug("s1").debugWithThread("s1-thread").doOnSuccess { latch.countDown() }.autoSubscribe()
        latch.await()
        Assert.assertTrue(disposable.isDisposed)

        latch = CountDownLatch(1)
        disposable = Single.error<Int>(RuntimeException()).debug("s2").debugWithThread("s2-thread").doOnError { latch.countDown() }.autoSubscribe()
        latch.await()
        Assert.assertTrue(disposable.isDisposed)

    }

    @Test
    fun test007() {
        Single.just(listOf(1, 2, 3, 4, 5)).mapList { it * 2 }.test().await().assertComplete().assertValues(listOf(2, 4, 6, 8, 10))
    }

    @Test
    fun test008() {
        Single.just(listOf(1, 2, 3, 4, 5)).firstInList().test().await().assertComplete().assertValue(1)
        Single.just(emptyList<Int>()).firstInList().test().await().assertComplete().assertNoValues()

        Single.just(listOf(null, 1)).firstInList().test().await().assertError(NullPointerException::class.java)
        Single.just(listOf(null, null)).firstInList().test().await().assertError(NullPointerException::class.java)
    }

    @Test
    fun test009() {
        Single.just(listOf(1, 2, 3, 4, 5)).firstInListNotNull().test().await().assertComplete().assertValue(1)
        Single.just(emptyList<Int>()).firstInListNotNull().test().await().assertComplete().assertNoValues()

        Single.just(listOf(null, 1)).firstInListNotNull().test().await().assertComplete().assertValue(1)
        Single.just(listOf(null, null)).firstInListNotNull().test().await().assertComplete().assertNoValues()
    }

    @Test
    fun test010() {
        Single.just(listOf(1, 2, 3, 4, 5)).asObservable().test().await().assertComplete().assertValues(1, 2, 3, 4, 5)
        Single.just(emptyList<Int>()).asObservable().test().await().assertComplete().assertNoValues()
        Single.just(listOf(1, 2, 3, null, 5)).asObservable().test().await().assertComplete().assertValues(1, 2, 3, 5)
        Single.just(listOf(1, 2, 3, null)).asObservable().test().await().assertComplete().assertValues(1, 2, 3)
        Single.just(listOf(null, null)).asObservable().test().await().assertComplete().assertNoValues()
    }

    @Test
    fun test011() {
        Single.just(listOf(1, 2, 3, 4, 5)).firstInList { it % 2 == 0 }.test().await().assertComplete().assertValue(2)
        Single.just(emptyList<Int>()).firstInList { it % 2 == 0 }.test().await().assertComplete().assertNoValues()
        Single.just(listOf(null, 1, 2)).firstInList { null != it && it % 2 == 0 }.test().await().assertComplete().assertValue(2)
        Single.just(listOf<Int?>(null, null)).firstInList { null != it && it % 2 == 0 }.test().await().assertComplete().assertNoValues()
    }


    @Test
    fun test012() {
        val now = System.currentTimeMillis()
        val finalTime = AtomicLong()
        val maxAttempts = 5

        Single.create<String> { emitter ->
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
    fun test013() {
        val now = System.currentTimeMillis()
        val finalTime = AtomicLong()
        val maxAttempts = 10

        Single.create<String> { emitter ->
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
