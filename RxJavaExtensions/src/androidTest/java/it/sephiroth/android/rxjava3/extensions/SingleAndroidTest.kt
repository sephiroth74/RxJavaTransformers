package it.sephiroth.android.rxjava3.extensions

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import it.sephiroth.android.rxjava3.extensions.observers.AutoDisposableSingleObserver
import it.sephiroth.android.rxjava3.extensions.single.*
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 02.03.21 - 16:50
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class SingleAndroidTest {

    @Test
    fun test01() {
        Single.just(listOf(1, 2, 3, 4, 5)).firstInList().test().await().assertValue(1)
    }

    @Test
    fun test02() {
        Single.just(listOf(1, 2, 3, 4, 5)).mapList { it * it }.test().await()
            .assertValues(listOf(1, 4, 9, 16, 25))
    }

    @Test
    fun test03() {
        val countDownLatch = CountDownLatch(2)
        Single.just(1).autoSubscribe {
            doOnSuccess { countDownLatch.countDown() }
            doOnStart { countDownLatch.countDown() }
        }

        countDownLatch.await(1, TimeUnit.SECONDS)
        Assert.assertEquals(0, countDownLatch.count)
    }

    @Test
    fun test04() {
        val mainThread = Looper.getMainLooper().thread

        val result = mutableListOf<String>()
        val s = Single.just(1).observeMain()

        val d = s.autoSubscribe(AutoDisposableSingleObserver {
            doOnStart {
                result.add("start")
            }
            doOnSuccess {
                Assert.assertEquals(mainThread, Thread.currentThread())
                result.add("success:$it")
            }
            doOnError {
                println("current thread: ${Thread.currentThread()}")
                Assert.assertEquals(mainThread, Thread.currentThread())
                result.add("error")
            }
        })

        s.test().await().assertValue(1)
        Assert.assertEquals(listOf("start", "success:1"), result)
        d.dispose()
    }

    @Test
    fun test05() {
        val mainThread = Looper.getMainLooper().thread
        val result = mutableListOf<String>()
        val s = Single.error<Int>(IllegalStateException("test")).observeMain()

        val d = s.autoSubscribe(AutoDisposableSingleObserver {
            doOnStart {
                result.add("start")
            }
            doOnSuccess {
                Assert.assertEquals(mainThread, Thread.currentThread())
                result.add("success:$it")
            }
            doOnError {
                Assert.assertEquals(mainThread, Thread.currentThread())
                result.add("error:${it.message}")
            }
        })

        s.test().await().assertError(IllegalStateException::class.java)
        Assert.assertEquals(listOf("start", "error:test"), result)
        d.dispose()
    }

    @Test
    fun test06() {
        val s1 = Single.just(1).debug("myClass")
        val d1 = s1.subscribe()
        s1.test()
        d1.dispose()

        val s2 = Single.just(1).debugWithThread("myClass")
        val d2 = s2.subscribe()
        s2.test()
        d2.dispose()

        Single.error<Int>(IllegalStateException("test")).debug("myClass").test().assertError(IllegalStateException::class.java)
        Single.error<Int>(IllegalStateException("test")).debugWithThread("myClass").test().assertError(IllegalStateException::class.java)
    }
}
