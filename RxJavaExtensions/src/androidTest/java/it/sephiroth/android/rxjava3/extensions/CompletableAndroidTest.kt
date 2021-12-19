package it.sephiroth.android.rxjava3.extensions

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import it.sephiroth.android.rxjava3.extensions.completable.*
import it.sephiroth.android.rxjava3.extensions.observers.AutoDisposableCompletableObserver
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


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
    fun test01() {
        val now = System.currentTimeMillis()
        val latch = CountDownLatch(1)
        delay(1, TimeUnit.SECONDS) {
            latch.countDown()
        }

        latch.await(2, TimeUnit.SECONDS)
        Assert.assertEquals(0, latch.count)
        println("totalTime: ${System.currentTimeMillis() - now}")
        Assert.assertTrue(System.currentTimeMillis() - now > 1000)
    }

    @Test
    fun test02() {
        val latch = CountDownLatch(2)
        Completable.create { it.onComplete() }
            .autoSubscribe {
                doOnStart { latch.countDown() }
                doOnComplete { latch.countDown() }
            }

        latch.await(1, TimeUnit.SECONDS)
        Assert.assertEquals(0, latch.count)
    }

    @Test
    fun test03() {
        val latch = CountDownLatch(2)
        Completable.create { it.onComplete() }
            .autoSubscribe(AutoDisposableCompletableObserver {
                doOnStart {
                    latch.countDown()
                }
                doOnComplete {
                    latch.countDown()
                }
            })

        latch.await(1, TimeUnit.SECONDS)
        Assert.assertEquals(0, latch.count)
    }

    @Test
    fun test04() {
        val looper = Looper.getMainLooper()
        Completable.create { emitter ->
            Assert.assertTrue(Thread.currentThread() != looper.thread)
            emitter.onComplete()
        }
            .subscribeOn(Schedulers.computation())
            .observeMain()
            .doOnComplete {
                Assert.assertTrue(Thread.currentThread() == looper.thread)
            }
            .test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertComplete()
    }

    @Test
    fun test05() {
        Completable
            .timer(20, TimeUnit.MILLISECONDS)
            .debug("completable")
    }

    @Test
    fun test06() {
        val c1 = Completable.complete().debug("myCompletable")
        val d1 = c1.autoSubscribe()

        c1.test().assertComplete()
        d1.dispose()

        Completable.fromAction { }
            .debugWithThread("myCompletable")
            .test()
            .assertComplete()

        Completable.error(IllegalStateException("test")).debug("myCompletable")
            .test().assertError(IllegalStateException::class.java)

        Completable.error(IllegalStateException("test")).debugWithThread("myCompletable")
            .test().assertError(IllegalStateException::class.java)
    }

    @Test
    fun test07() {
        val latch1 = CountDownLatch(2)
        val result1 = mutableListOf<String>()

        val c1 = Completable.create { emitter -> emitter.onComplete() }
        c1.autoSubscribe {
            doOnStart {
                result1.add("onStart")
                latch1.countDown()
            }
            doOnComplete {
                result1.add("onComplete")
                latch1.countDown()
            }
        }

        c1.test().assertComplete()
        latch1.await()

        Assert.assertEquals(listOf("onStart", "onComplete"), result1)
    }

    @Test
    fun test08() {
        val latch1 = CountDownLatch(2)
        val result1 = mutableListOf<String>()

        val c1 = Completable.error(IllegalStateException("test exception")).debugWithThread("testCompletable")
        c1.autoSubscribe {
            doOnError {
                result1.add("onError:${it.message}")
                latch1.countDown()
            }

            doOnStart {
                result1.add("onStart")
                latch1.countDown()
            }

            doOnComplete {
                result1.add("onComplete")
                latch1.countDown()
            }
        }

        c1.test().assertError(IllegalStateException::class.java)
        latch1.await()

        Assert.assertEquals(listOf("onStart", "onError:test exception"), result1)
    }

    @Test
    fun test09() {
        val c = Completable.complete()
        val s = c.autoSubscribe()

        Assert.assertTrue(s.isDisposed)
    }
}
