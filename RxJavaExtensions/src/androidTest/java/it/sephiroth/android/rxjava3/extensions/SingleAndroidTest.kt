package it.sephiroth.android.rxjava3.extensions

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.functions.Function
import io.reactivex.rxjava3.schedulers.Schedulers
import it.sephiroth.android.rxjava3.extensions.observers.AutoDisposableSingleObserver
import it.sephiroth.android.rxjava3.extensions.single.*
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch


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
    }

    @Test
    fun test009() {
        Single.just(listOf(1, 2, 3, 4, 5)).asObservable().test().await().assertComplete().assertValues(1, 2, 3, 4, 5)
        Single.just(emptyList<Int>()).asObservable().test().await().assertComplete().assertNoValues()
        Single.just(listOf(1, 2, 3, null, 5)).asObservable().test().await().assertComplete().assertValues(1, 2, 3, 5)
        Single.just(listOf(1, 2, 3, null)).asObservable().test().await().assertComplete().assertValues(1, 2, 3)
        Single.just(listOf(null, null)).asObservable().test().await().assertComplete().assertNoValues()
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
