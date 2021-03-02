package it.sephiroth.android.rxjava2.extensions

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import it.sephiroth.android.rxjava2.extensions.observable.*
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 02.03.21 - 13:47
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class ObservableAndroidTest {
    @Test
    fun test01() {
        val o = Observable.just(1, 2, 3, 4, 5)
            .toSingle()
            .test()
            .await()

        o.assertComplete()
        o.assertResult(1)
    }

    @Test
    fun test02() {
        val o = Observable
            .just(listOf(1, 2, 3, 4, 5), listOf(6, 7, 8, 9, 10))
            .firstInList()
            .test()
            .awaitCount(1)

        o.assertResult(1)
    }

    @Test
    fun test03() {
        val latch = CountDownLatch(6)
        val d = Observable
            .just(1, 2, 3, 4, 5)
            .autoSubscribe {
                doOnNext {
                    latch.countDown()
                }
                doOnComplete {
                    latch.countDown()
                }
            }

        latch.await(1, TimeUnit.SECONDS)
        Assert.assertEquals(0, latch.count)
        Assert.assertTrue(d.isDisposed)
    }

    @Test
    fun test04() {
        val now = System.currentTimeMillis()
        val counter = AtomicInteger(0)
        val latch = CountDownLatch(1)
        Observable
            .just(1)
            .refreshEvery(50, TimeUnit.MILLISECONDS, Schedulers.newThread())
            .autoSubscribe {
                doOnNext {
                    if (counter.incrementAndGet() > 9) {
                        dispose()
                        latch.countDown()
                    }
                }

            }

        latch.await(1, TimeUnit.SECONDS)
        Assert.assertEquals(10, counter.get())

        val totalTime = System.currentTimeMillis() - now
        println("totalTime: $totalTime")
        Assert.assertTrue(totalTime >= 450)
    }

    @Test
    fun test05() {
        val initial = System.currentTimeMillis()
        var now = System.currentTimeMillis()
        val counter = AtomicInteger(0)
        val publisher = PublishSubject.create<Boolean>()

        val o = Observable
            .just(1)
            .subscribeOn(Schedulers.newThread())
            .autoRefresh(publisher)
            .doOnNext { counter.incrementAndGet() }
            .test()

        while (true) {
            if (System.currentTimeMillis() - initial > 550) {
                break
            }

            if (System.currentTimeMillis() - now > 50) {
                publisher.onNext(true)
                now = System.currentTimeMillis()
            }
        }

        o.awaitCount(10)

        Assert.assertEquals(10, counter.get())
    }

    @Test
    fun test06() {
        val retryCount = AtomicInteger(0)
        val errorCount = AtomicInteger(0)
        val counter = AtomicInteger(0)
        var count = 0

        Observable.create<Int> { emitter ->
            while (true) {
                count += 1
                if (count % 2 == 0) throw IllegalStateException("Illegal even number")
                emitter.onNext(count)
                if (count >= 10) emitter.onComplete()
                Thread.sleep(1000)
            }
        }.subscribeOn(Schedulers.newThread())
            .retry(
                predicate = {
                    println("[${System.currentTimeMillis()}] predicate running...")
                    retryCount.incrementAndGet()
                    true
                },
                delayBeforeRetry = 1,
                maxRetry = 1,
                timeUnit = TimeUnit.SECONDS
            )
            .doOnNext {
                counter.set(counter.get() + it)
                println("[${System.currentTimeMillis()}] onNext: $it")
            }
            .doOnError {
                println("[${System.currentTimeMillis()}] onError: $it")
                errorCount.incrementAndGet()
            }
            .test()
            .awaitTerminalEvent(10, TimeUnit.SECONDS)

        Assert.assertEquals(2, retryCount.get())
        Assert.assertEquals(1, errorCount.get())
        Assert.assertEquals(4, counter.get())

    }

    @Test
    fun test07() {
        Observable.just(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
            .mapList { it * 2 }
            .test()
            .assertValue(listOf(2, 4, 6, 8, 10, 12, 14, 16, 18, 20))
            .awaitTerminalEvent(1, TimeUnit.SECONDS)
    }

    @Test
    fun test08() {
        val now = System.currentTimeMillis()

        Observable.just(1, 2, 3, 4, 5)
            .muteUntil(func = {
                println("[${System.currentTimeMillis() - now}] mute until predicate")
                System.currentTimeMillis() - now < 1000
            }, delay = 100, unit = TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.newThread())
            .test()
            .await()
            .assertValueCount(5)
            .assertValueAt(4, 5)
            .awaitTerminalEvent(2, TimeUnit.SECONDS)

        val totalTime = System.currentTimeMillis() - now
        println("totalTime: $totalTime")

        Assert.assertTrue(totalTime > 1000)
    }
}
