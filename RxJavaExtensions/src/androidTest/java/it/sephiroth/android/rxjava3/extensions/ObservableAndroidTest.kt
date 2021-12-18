package it.sephiroth.android.rxjava3.extensions

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import it.sephiroth.android.rxjava3.extensions.observable.*
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
        val counter = AtomicInteger(0)
        val publisher = PublishSubject.create<Boolean>()

        val o = Observable
            .just(1)
            .subscribeOn(Schedulers.newThread())
            .autoRefresh(publisher)
            .doOnNext { counter.incrementAndGet() }
            .test()


        Thread.sleep(10)
        publisher.onNext(true)

        Thread.sleep(10)
        publisher.onNext(false)

        Thread.sleep(10)
        publisher.onNext(true)

        o.awaitCount(2)

        Assert.assertEquals(2, counter.get())
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
            .awaitDone(10, TimeUnit.SECONDS)

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
            .awaitDone(1, TimeUnit.SECONDS)
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
            .awaitDone(2, TimeUnit.SECONDS)

        val totalTime = System.currentTimeMillis() - now
        println("totalTime: $totalTime")

        Assert.assertTrue(totalTime > 1000)
    }

    @Test
    fun test09() {
        val latch = CountDownLatch(7)
        val values = mutableListOf<Long>()
        val now = System.currentTimeMillis()
        var totalTime = now
        ObservableUtils.countDown(0, 5, 1, TimeUnit.SECONDS,
            { value ->
                println("onNext(time=" + (System.currentTimeMillis() - now) + ", value=" + value + ")")
                values.add(value)
                latch.countDown()
            },
            {
                println("onComplete(time=" + (System.currentTimeMillis() - now) + ")")
                totalTime = System.currentTimeMillis()
                latch.countDown()
            })

        latch.await(10, TimeUnit.SECONDS)

        val timePassed = totalTime - now

        Assert.assertTrue(values.size == 6)
        Assert.assertEquals(listOf(0L, 1L, 2L, 3L, 4L, 5L), values)
        Assert.assertTrue("$timePassed should be greater than 6000", timePassed >= 5000)
        Assert.assertTrue("$timePassed should be less than 7000", timePassed < 6000)
    }

    @Test
    fun test10() {
        val latch = CountDownLatch(6)
        val values = mutableListOf<Long>()
        val now = System.currentTimeMillis()
        var totalTime = now
        ObservableUtils.countDown(5, 1, 1, TimeUnit.SECONDS,
            { value ->
                println("onNext(time=" + (System.currentTimeMillis() - now) + ", value=" + value + ")")
                values.add(value)
                latch.countDown()
            },
            {
                println("onComplete(time=" + (System.currentTimeMillis() - now) + ")")
                totalTime = System.currentTimeMillis()
                latch.countDown()
            })

        latch.await(10, TimeUnit.SECONDS)

        val timePassed = totalTime - now

        Assert.assertTrue(values.size == 5)
        Assert.assertEquals(listOf(5L, 4L, 3L, 2L, 1L), values)
        Assert.assertTrue("$timePassed should be greater than 4000", timePassed >= 4000)
        Assert.assertTrue("$timePassed should be less than 5000", timePassed < 5000)
    }

    @Test
    fun test11() {
        val now = System.currentTimeMillis()
        val latch = CountDownLatch(100)
        val values = mutableListOf<Long>()
        ObservableUtils.timer(
            1,
            TimeUnit.SECONDS,
            10,
            TimeUnit.MILLISECONDS,
            onTick = { step, stepValue ->
                values.add(step)
                latch.countDown()

            }, null
        )

        latch.await(2, TimeUnit.SECONDS)
        val end = System.currentTimeMillis()

        Assert.assertTrue(values.size == 100)
        Assert.assertEquals(LongRange(1, 100).toList(), values)
        Assert.assertTrue(end - now >= 1000)
        Assert.assertTrue(end - now < 2000)
    }

    @Test
    fun test12() {
        Observable
            .just(1, 2, 3, 4, 5, 6, 7)
            .mapNotNull { it ->
                if (it % 2 == 0) it
                else null
            }.test().assertValues(2, 4, 6)

        Observable
            .just(1, 2, 3, 4, 5, 6)
            .mapNotNull { null }
            .test()
            .assertNoValues()
            .assertComplete()

    }
}
