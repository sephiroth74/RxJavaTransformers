package it.sephiroth.android.rxjava3.extensions

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import io.reactivex.rxjava3.core.Maybe
import it.sephiroth.android.rxjava3.extensions.maybe.autoSubscribe
import it.sephiroth.android.rxjava3.extensions.maybe.observeMain
import it.sephiroth.android.rxjava3.extensions.observers.AutoDisposableMaybeObserver
import org.junit.Assert
import org.junit.Test
import org.junit.internal.runners.statements.Fail
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 02.03.21 - 16:50
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class MaybeAndroidTest {
    @Test
    fun test01() {

        val result = mutableListOf<String>()
        val countDownLatch = CountDownLatch(1)

        Maybe.just(1).autoSubscribe {
            doOnStart { result.add("start") }
            doOnSuccess { result.add("success") }
            doOnComplete { result.add("complete") }
            doOnError { result.add("error") }
            doOnFinish {
                result.add("finish")
                countDownLatch.countDown()
            }
        }

        countDownLatch.await(1, TimeUnit.SECONDS)
        Assert.assertEquals(0, countDownLatch.count)
        Assert.assertEquals(result, mutableListOf("start", "success", "finish"))
    }

    @Test
    fun test02() {

        val result = mutableListOf<String>()
        val countDownLatch = CountDownLatch(1)

        Maybe.empty<Int>().autoSubscribe {
            doOnStart { result.add("start") }
            doOnSuccess { result.add("success") }
            doOnComplete { result.add("complete") }
            doOnError { result.add("error") }
            doOnFinish {
                result.add("finish")
                countDownLatch.countDown()
            }
        }

        countDownLatch.await(1, TimeUnit.SECONDS)
        Assert.assertEquals(0, countDownLatch.count)
        Assert.assertEquals(result, mutableListOf("start", "complete", "finish"))
    }

    @Test
    fun test03() {
        val result = mutableListOf<String>()
        val countDownLatch = CountDownLatch(1)

        Maybe.error<Int>(IllegalStateException("test exception")).autoSubscribe {
            doOnStart { result.add("start") }
            doOnSuccess { result.add("success") }
            doOnComplete { result.add("complete") }
            doOnError { result.add("error") }
            doOnFinish {
                result.add("finish")
                countDownLatch.countDown()
            }
        }

        countDownLatch.await(1, TimeUnit.SECONDS)
        Assert.assertEquals(0, countDownLatch.count)
        Assert.assertEquals(result, mutableListOf("start", "error", "finish"))
    }

    @Test
    fun test04() {
        val mainThread = Looper.getMainLooper().thread
        val currentThread = Thread.currentThread()
        val result = mutableListOf<String>()

        val m = Maybe.just(1).observeMain()
        m.autoSubscribe {
            doOnStart {
                result.add("start")
                Assert.assertEquals(currentThread, Thread.currentThread())
            }
            doOnSuccess {
                result.add("success")
                Assert.assertEquals(mainThread, Thread.currentThread())
            }
            doOnFinish {
                result.add("finish")
                Assert.assertEquals(mainThread, Thread.currentThread())
            }
            doOnComplete {
                result.add("complete")
                Assert.assertEquals(mainThread, Thread.currentThread())
            }
            doOnError {
                result.add("error")
                Assert.assertEquals(mainThread, Thread.currentThread())
            }
        }

        m.test().await().assertValue(1)
        Assert.assertEquals(listOf("start", "success", "finish"), result)
    }


    @Test
    fun test05() {
        val mainThread = Looper.getMainLooper().thread
        val currentThread = Thread.currentThread()
        val result = mutableListOf<String>()

        val m = Maybe.empty<Int>().observeMain()
        m.autoSubscribe {
            doOnStart {
                result.add("start")
                Assert.assertEquals(currentThread, Thread.currentThread())
            }
            doOnSuccess {
                result.add("success")
                Assert.assertEquals(mainThread, Thread.currentThread())
            }
            doOnFinish {
                result.add("finish")
                Assert.assertEquals(mainThread, Thread.currentThread())
            }
            doOnComplete {
                result.add("complete")
                Assert.assertEquals(mainThread, Thread.currentThread())
            }
            doOnError {
                result.add("error")
                Assert.assertEquals(mainThread, Thread.currentThread())
            }
        }

        m.test().await().assertNoValues()
        Assert.assertEquals(listOf("start", "complete", "finish"), result)
    }

    @Test
    fun test06() {
        val mainThread = Looper.getMainLooper().thread
        val currentThread = Thread.currentThread()
        val result = mutableListOf<String>()

        val m = Maybe.error<Int>(RuntimeException("test")).observeMain()
        m.autoSubscribe {
            doOnStart {
                result.add("start")
                Assert.assertEquals(currentThread, Thread.currentThread())
            }
            doOnSuccess {
                result.add("success")
                Assert.assertEquals(mainThread, Thread.currentThread())
            }
            doOnFinish {
                result.add("finish")
                Assert.assertEquals(mainThread, Thread.currentThread())
            }
            doOnComplete {
                result.add("complete")
                Assert.assertEquals(mainThread, Thread.currentThread())
            }
            doOnError {
                result.add("error")
                Assert.assertEquals(mainThread, Thread.currentThread())
            }
        }

        m.test().await().assertError(RuntimeException::class.java)
        Assert.assertEquals(listOf("start", "error", "finish"), result)
    }

    @Test
    fun test07() {
        val result = AtomicInteger()
        val m = Maybe.just(1)
        m.autoSubscribe(AutoDisposableMaybeObserver() {
            doOnSuccess { result.getAndSet(it) }
        })

        m.test().await().assertValue(1)
        Assert.assertEquals(1, result.get())
    }
}
