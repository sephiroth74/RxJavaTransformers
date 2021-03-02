package it.sephiroth.android.rxjava2.extensions

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import it.sephiroth.android.rxjava2.extensions.completable.autoSubscribe
import it.sephiroth.android.rxjava2.extensions.completable.debug
import it.sephiroth.android.rxjava2.extensions.completable.delay
import it.sephiroth.android.rxjava2.extensions.completable.observeMain
import it.sephiroth.android.rxjava2.extensions.observers.AutoDisposableCompletableObserver
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
            .autoSubscribe(AutoDisposableCompletableObserver() {
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
}
