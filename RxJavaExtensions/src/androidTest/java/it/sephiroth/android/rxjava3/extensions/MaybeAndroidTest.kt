package it.sephiroth.android.rxjava3.extensions

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.schedulers.Schedulers
import it.sephiroth.android.rxjava3.extensions.maybe.autoSubscribe
import it.sephiroth.android.rxjava3.extensions.maybe.debug
import it.sephiroth.android.rxjava3.extensions.maybe.debugWithThread
import it.sephiroth.android.rxjava3.extensions.maybe.observeMain
import it.sephiroth.android.rxjava3.extensions.observers.AutoDisposableMaybeObserver
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

