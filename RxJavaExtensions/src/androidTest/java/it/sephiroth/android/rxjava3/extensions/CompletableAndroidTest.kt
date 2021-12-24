package it.sephiroth.android.rxjava3.extensions

import android.os.Looper
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableSource
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
        Completable.error(RuntimeException("test exception")).autoSubscribe(AutoDisposableCompletableObserver {
            doOnStart { result.add("start") }
            doOnComplete { result.add("complete") }
            doOnError { result.add("error"); latch.countDown() }
        })

        latch.await()
        Assert.assertEquals(listOf("start", "error"), result)
    }


    @Test
    fun test009() {
        Completable.complete().debug("c1").debugWithThread("c1-tread").test().await().assertComplete()
        Completable.error(RuntimeException("test")).debug("c2").debugWithThread("c2-tread").test().await().assertError(RuntimeException::class.java)

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

}
