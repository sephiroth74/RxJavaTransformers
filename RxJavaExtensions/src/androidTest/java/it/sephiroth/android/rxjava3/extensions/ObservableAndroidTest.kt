package it.sephiroth.android.rxjava3.extensions

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import it.sephiroth.android.rxjava3.extensions.observable.*
import it.sephiroth.android.rxjava3.extensions.observers.AutoDisposableObserver
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 02.03.21 - 13:47
 */
@RunWith(AndroidJUnit4::class)
class ObservableAndroidTest {
    @Test
    fun test001() {
        Observable.just(1, 2, 3, 4, 5).toSingle().test().await().assertComplete().assertResult(1)
    }

    @Test
    fun test002() {
        Observable.just(listOf(1, 2, 3, 4, 5)).firstInList().test().await().assertComplete().assertResult(1)
        Observable.just(emptyList<Int>()).firstInList().test().await().assertComplete().assertNoValues()

        Observable.just(listOf(null, 1)).firstInList().test().await().assertError(NullPointerException::class.java)
        Observable.just(listOf(null, null)).firstInList().test().await().assertError(NullPointerException::class.java)
    }

    @Test
    fun test002b() {
        Observable.just(listOf(1, 2, 3, 4, 5)).firstInListNotNull().test().await().assertComplete().assertValue(1)
        Observable.just(emptyList<Int>()).firstInListNotNull().test().await().assertComplete().assertNoValues()

        Observable.just(listOf(null, 1)).firstInListNotNull().test().await().assertComplete().assertValue(1)
        Observable.just(listOf(null, null)).firstInListNotNull().test().await().assertComplete().assertNoValues()
    }

    @Test
    fun test03() {
        val latch = CountDownLatch(6)
        val d = Observable
            .just(1, 2, 3, 4, 5)
            .autoSubscribe {
                doOnNext { latch.countDown() }
                doOnComplete { latch.countDown() }
            }

        latch.await()
        Assert.assertTrue(d.isDisposed)
    }

    @Test
    fun test04() {
        val now = System.currentTimeMillis()
        val counter = AtomicInteger(0)
        val latch = CountDownLatch(1)
        Observable
            .just(1)
            .refreshEvery(50, TimeUnit.MILLISECONDS)
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
            }
            .test()
            .assertValues(2, 4, 6)
            .assertComplete()

        Observable
            .just(1, 2, 3, 4, 5, 6)
            .mapNotNull { null }
            .test()
            .assertNoValues()
            .assertComplete()


        val actual = AtomicInteger()
        val max = 5
        val o = Observable.create<Int> { emitter ->
            do {
                val current = actual.getAndSet(actual.get() + 1)
                if (current < max) {
                    emitter.onNext(actual.get())
                } else if (current == max) {
                    emitter.onComplete()
                } else {
                    println("Exceeded!")
                }
            } while (actual.get() <= max && !emitter.isDisposed)
        }

        o.test().await().assertValues(1, 2, 3, 4, 5).assertComplete()

        actual.set(5)
        o.mapNotNull { it % 2 == 0 }.test().assertNoValues()


        Observable.just(1, 2, 3).mapNotNull {
            throw RuntimeException("test exception")
        }.test().assertError(RuntimeException::class.java).await()

    }

    @Test
    fun test13() {
        val currentThread = Thread.currentThread()

        var latch = CountDownLatch(1)
        val targetThreadName = AtomicReference<String>()

        Schedulers.single().scheduleDirect {
            targetThreadName.set(Thread.currentThread().name)
            latch.countDown()
        }

        latch.await()

        latch = CountDownLatch(1)

        val result = mutableListOf<String>()
        val o = Observable.just(1, 2)
        o.subscribeOn(Schedulers.io())
            .observeOn(Schedulers.single())
            .autoSubscribe(AutoDisposableObserver {
                doOnStart {
                    println("onStart")
                    result.add("start")
                    Assert.assertEquals(currentThread, Thread.currentThread())
                }
                doOnNext {
                    println("onNext")
                    result.add("next")
                    Assert.assertEquals(targetThreadName.get(), Thread.currentThread().name)
                }
                doOnComplete {
                    println("onComplete")
                    result.add("complete")
                    Assert.assertEquals(targetThreadName.get(), Thread.currentThread().name)
                    latch.countDown()
                }
                doOnError {
                    println("onError")
                    result.add("error")
                    Assert.assertEquals(targetThreadName.get(), Thread.currentThread().name)
                }
            })

        o.test().await().assertComplete()
        latch.await()
        Assert.assertEquals(listOf("start", "next", "next", "complete"), result)
    }


    @Test
    fun test14() {
        val currentThread = Thread.currentThread()

        var latch = CountDownLatch(1)
        val targetThreadName = AtomicReference<String>()

        Schedulers.single().scheduleDirect {
            targetThreadName.set(Thread.currentThread().name)
            latch.countDown()
        }

        latch.await()

        latch = CountDownLatch(1)

        val result = mutableListOf<String>()
        val o = Observable.error<Int>(RuntimeException("test"))
        o.subscribeOn(Schedulers.io())
            .observeOn(Schedulers.single())
            .autoSubscribe(AutoDisposableObserver {
                doOnStart {
                    result.add("start")
                    Assert.assertEquals(currentThread, Thread.currentThread())
                }
                doOnNext {
                    result.add("next")
                    Assert.assertEquals(targetThreadName.get(), Thread.currentThread().name)
                }
                doOnComplete {
                    result.add("complete")
                    println("${Thread.currentThread()}")
                    Assert.assertEquals(targetThreadName.get(), Thread.currentThread().name)
                }
                doOnError {
                    result.add("error")
                    Assert.assertEquals(targetThreadName.get(), Thread.currentThread().name)
                    latch.countDown()
                }
            })

        o.test().await().assertError(RuntimeException::class.java)
        latch.await()
        Assert.assertEquals(listOf("start", "error"), result)
    }

    @Test
    fun test15() {
        Observable.just(1).debug("o1").debugWithThread("o1-thread").test().await()
        Observable.error<Int>(RuntimeException("test")).debug("o2").debugWithThread("o2-thread").test().assertError(RuntimeException::class.java)
    }

    @Test
    fun test16() {
        val l1 = CountDownLatch(1)
        val latch = CountDownLatch(1)

        val m1 = Observable.create<Int> { emitter ->
            l1.await()
            if (!emitter.isDisposed) emitter.onNext(1)
        }.subscribeOn(Schedulers.computation())
            .debug("o1")
            .debugWithThread("o1-thread")

        val s1 = m1.subscribe()

        Schedulers.single().scheduleDirect {
            Thread.sleep(20)
            l1.countDown()
            s1.dispose()
            latch.countDown()
        }

        latch.await()
    }

    @Test
    fun test17() {
        val source = BehaviorSubject.createDefault(true)

        val latch = CountDownLatch(1)
        val now = System.currentTimeMillis()
        val elapsed = AtomicLong()
        val pausedSource = source.toSerialized().share()

        val o = ObservableUtils.pausedTimer(300, TimeUnit.MILLISECONDS, pausedSource)
            .doOnComplete {
                elapsed.set(System.currentTimeMillis() - now)
                println("timer::onComplete: ${System.currentTimeMillis() - now}")
                latch.countDown()
            }

        Schedulers.single().scheduleDirect {
            println("opening gate...")
            Thread.sleep(200)
            source.onNext(false)
        }

        o.test().await()
        latch.await()

        Assert.assertTrue(elapsed.get() > 500)
    }

    @Test
    fun test18() {
        // pausedInterval
        val source = BehaviorSubject.createDefault(true)
        val pausedSource = source.toSerialized().share()

        val latch = CountDownLatch(1)
        var now = System.currentTimeMillis()
        val elapsed = AtomicLong()
        val o = ObservableUtils.pausedInterval(50, TimeUnit.MILLISECONDS, pausedSource)
            .doOnSubscribe {
                now = System.currentTimeMillis()
            }
            .doOnNext {
                elapsed.set(System.currentTimeMillis() - now)
                println("timer.next = ${System.currentTimeMillis() - now}")
                latch.countDown()
            }
            .doOnComplete {
                println("timer.complete = ${System.currentTimeMillis() - now}")
            }


        Schedulers.single().scheduleDirect {
            Thread.sleep(500)
            println("opening gate")
            source.onNext(false)
        }

        val s = o.subscribe()
        latch.await()
        s.dispose()

        Assert.assertTrue(elapsed.get() in 501..600)

    }
}
