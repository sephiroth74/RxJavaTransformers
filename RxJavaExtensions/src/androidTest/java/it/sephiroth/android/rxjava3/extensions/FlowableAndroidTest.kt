package it.sephiroth.android.rxjava3.extensions

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import it.sephiroth.android.rxjava3.extensions.completable.delay
import it.sephiroth.android.rxjava3.extensions.flowable.*
import it.sephiroth.android.rxjava3.extensions.observers.AutoDisposableSubscriber
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 01.03.21 - 10:13
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class FlowableAndroidTest {

    @Test
    fun test001() {
        val latch = CountDownLatch(8)
        val d = Flowable
            .just(1, 2, 3, 4, 5).autoSubscribe {
                doOnStart { latch.countDown() }
                doOnNext { latch.countDown() }
                doOnComplete { latch.countDown() }
                doOnFinish { latch.countDown() }
            }
        latch.await()
        Assert.assertTrue(d.isDisposed)
    }

    @Test
    fun test002() {
        val currentThread = Thread.currentThread()
        val latch = CountDownLatch(1)

        val result = mutableListOf<String>()
        val flowable = Flowable.just(1, 2)
        flowable.subscribeOn(Schedulers.io())
            .observeOn(Schedulers.single())
            .autoSubscribe(AutoDisposableSubscriber {
                doOnStart {
                    result.add("start")
                    Assert.assertEquals(currentThread, Thread.currentThread())
                }
                doOnNext {
                    result.add("next:$it")
                    Assert.assertEquals(singleThread, Thread.currentThread())
                }
                doOnComplete {
                    result.add("complete")
                    Assert.assertEquals(singleThread, Thread.currentThread())
                }
                doOnError {
                    result.add("error")
                    Assert.assertEquals(singleThread, Thread.currentThread())
                }
                doOnFinish {
                    result.add("finish")
                    Assert.assertEquals(singleThread, Thread.currentThread())
                    latch.countDown()
                }
            })

        flowable.test().await().assertComplete()
        latch.await()
        Assert.assertEquals(listOf("start", "next:1", "next:2", "complete", "finish"), result)
    }

    @Test
    fun test003() {
        val currentThread = Thread.currentThread()
        val latch = CountDownLatch(1)
        val result = mutableListOf<String>()
        val flowable = Flowable.error<Int>(RuntimeException("test"))
        flowable.subscribeOn(Schedulers.io())
            .observeOn(Schedulers.single())
            .autoSubscribe(AutoDisposableSubscriber {
                doOnStart {
                    result.add("start")
                    Assert.assertEquals(currentThread, Thread.currentThread())
                }
                doOnNext {
                    result.add("next")
                    Assert.assertEquals(singleThread, Thread.currentThread())
                }
                doOnComplete {
                    result.add("complete")
                    Assert.assertEquals(singleThread, Thread.currentThread())
                }
                doOnError {
                    result.add("error")
                    Assert.assertEquals(singleThread, Thread.currentThread())
                }
                doOnFinish {
                    result.add("finish")
                    Assert.assertEquals(singleThread, Thread.currentThread())
                    latch.countDown()
                }
            })

        flowable.test().await().assertError(RuntimeException::class.java)
        latch.await()
        Assert.assertEquals(listOf("start", "error", "finish"), result)
    }

    @Test
    fun test004() {
        Flowable.just(1).debug("f1").debugWithThread("f1-thread").test().await()
        Flowable.error<Int>(RuntimeException("test")).debug("f2").debugWithThread("f2-thread").test().assertError(RuntimeException::class.java)
    }

    @Test
    fun test005() {
        val latch = CountDownLatch(1)
        val disposable = Flowable.just(1, 2)
            .delaySubscription(1, TimeUnit.SECONDS)
            .debug("f1").debugWithThread("f1-thread").autoSubscribe { }

        delay(16, TimeUnit.MILLISECONDS) {
            disposable.dispose()
            latch.countDown()
        }

        latch.await()
        Assert.assertTrue(disposable.isDisposed)
    }

    @Test
    fun test006() {
        val currentThread = Thread.currentThread()
        val latch = CountDownLatch(4)
        Flowable.just(1)
            .subscribeOn(Schedulers.io())
            .observeMain()
            .autoSubscribe {
                doOnStart {
                    Assert.assertEquals(currentThread, Thread.currentThread())
                    latch.countDown()
                }
                doOnError {
                    Assert.assertEquals(mainThread, Thread.currentThread())
                    latch.countDown()
                }
                doOnNext {
                    latch.countDown()
                    Assert.assertEquals(mainThread, Thread.currentThread())
                }
                doOnComplete {
                    latch.countDown()
                    Assert.assertEquals(mainThread, Thread.currentThread())
                }
                doOnFinish {
                    latch.countDown()
                    Assert.assertEquals(mainThread, Thread.currentThread())
                }
            }

        latch.await()
    }

    @Test
    fun test007() {
        val count = AtomicInteger(0)
        Flowable.create<Int>({ emitter ->
            emitter.onNext(count.getAndIncrement())
            Thread.sleep(50)
            emitter.onNext(count.getAndIncrement())
            emitter.onNext(count.getAndIncrement())
            Thread.sleep(250)
            emitter.onNext(count.getAndIncrement())
            Thread.sleep(50)
            emitter.onNext(count.getAndIncrement())
            Thread.sleep(250)
            emitter.onNext(count.getAndIncrement())
            emitter.onComplete()

        }, BackpressureStrategy.BUFFER)
            .subscribeOn(Schedulers.computation())
            .skipBetween(200, TimeUnit.MILLISECONDS, true)
            .test()
            .await()
            .assertComplete()
            .assertValues(0, 3, 5)
    }

    @Test
    fun test008() {
        val count = AtomicInteger(0)
        Flowable.create<Int>({ emitter ->
            emitter.onNext(count.getAndIncrement())
            Thread.sleep(50)
            emitter.onNext(count.getAndIncrement())
            emitter.onNext(count.getAndIncrement())
            Thread.sleep(250)
            emitter.onNext(count.getAndIncrement())
            Thread.sleep(50)
            emitter.onNext(count.getAndIncrement())
            Thread.sleep(250)
            emitter.onNext(count.getAndIncrement())
            emitter.onComplete()

        }, BackpressureStrategy.BUFFER)
            .subscribeOn(Schedulers.computation())
            .skipBetween(200, TimeUnit.MILLISECONDS, false)
            .test()
            .await()
            .assertComplete()
            .assertValues(3, 5)
    }

    @Test
    fun test009() {
        val subject = BehaviorSubject.create<ITestEvent>()
        val valueCount = CountDownLatch(5)
        val values = mutableListOf<ITestEvent>()

        val subscriber = subject.toFlowable(BackpressureStrategy.BUFFER)
            .pingPong(TestEvent01::class.java, TestEvent03::class.java)
            .doOnNext {
                println("onNext: $it")
                valueCount.countDown()
                values.add(it)
            }.test()

        subject.onNext(TestEvent01)
        subject.onNext(TestEvent02)
        subject.onNext(TestEvent03)
        subject.onNext(TestEvent02)
        subject.onNext(TestEvent01)
        subject.onNext(TestEvent01)
        subject.onNext(TestEvent01)
        subject.onNext(TestEvent03)
        subject.onNext(TestEvent03)
        subject.onNext(TestEvent02)
        subject.onNext(TestEvent01)
        subject.onComplete()

        subscriber.await()

        valueCount.await()

        Assert.assertEquals(
            listOf(
                TestEvent01,
                TestEvent03,
                TestEvent01,
                TestEvent03,
                TestEvent01
            ), values
        )
    }

    @Test
    fun test010() {
        Flowable.just(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
            .mapList { it * 2 }
            .test()
            .await().assertComplete()
            .assertValue(listOf(2, 4, 6, 8, 10, 12, 14, 16, 18, 20))
    }

    @Test
    fun test011() {
        Flowable.just(1, 2, 3, 4, 5, 6, 7)
            .mapNotNull { it ->
                if (it % 2 == 0) it
                else null
            }
            .test()
            .assertValues(2, 4, 6)
            .assertComplete()

        Flowable.just(1, 2, 3, 4, 5, 6, 7)
            .filter { it > 0 }
            .mapNotNull { it ->
                if (it % 2 == 0) it
                else null
            }
            .filter { it > 0 }
            .test()
            .assertValues(2, 4, 6)
            .assertComplete()

        Flowable.just(1, 2, 3, 4, 5, 6)
            .mapNotNull { null }
            .test()
            .await()
            .assertComplete()
            .assertNoValues()
            .assertComplete()


        val actual = AtomicInteger()
        val max = 5
        val flowable = Flowable.create<Int>({ emitter ->
            do {
                val current = actual.getAndSet(actual.get() + 1)
                if (current < max) {
                    emitter.onNext(actual.get())
                } else if (current == max) {
                    emitter.onComplete()
                } else {
                    println("Exceeded!")
                }
            } while (actual.get() <= max && !emitter.isCancelled)
        }, BackpressureStrategy.BUFFER)

        flowable.test().await().assertValues(1, 2, 3, 4, 5).assertComplete()

        actual.set(5)
        flowable.mapNotNull { it % 2 == 0 }.test().await().assertComplete().assertNoValues()


        Flowable.just(1, 2, 3).mapNotNull { throw RuntimeException("test exception") }.test().assertError(RuntimeException::class.java).await()

    }

    @Test
    fun test012() {
        Flowable.just(1, 2, 3, 4, 5).toSingle().test().await().assertComplete().assertResult(1)
    }

    @Test
    fun test013() {
        Flowable.just(listOf(1, 2, 3, 4, 5)).firstInList().test().await().assertComplete().assertResult(1)
        Flowable.just(emptyList<Int>()).firstInList().test().await().assertComplete().assertNoValues()

        Flowable.just(listOf(null, 1)).firstInList().test().await().assertError(NullPointerException::class.java)
        Flowable.just(listOf(null, null)).firstInList().test().await().assertError(NullPointerException::class.java)
    }

    @Test
    fun test014() {
        Flowable.just(listOf(1, 2, 3, 4, 5)).firstInListNotNull().test().await().assertComplete().assertValue(1)
        Flowable.just(emptyList<Int>()).firstInListNotNull().test().await().assertComplete().assertNoValues()
        Flowable.just(listOf(null, 1)).firstInListNotNull().test().await().assertComplete().assertValue(1)
        Flowable.just(listOf(null, null)).firstInListNotNull().test().await().assertComplete().assertNoValues()
    }

    @Test
    fun test015() {
        Flowable.just(listOf(1, 2, 3, 4, 5))
            .firstInList { it % 2 == 0 }
            .test().await().assertComplete().assertValue(2)

        Flowable.just(emptyList<Int>())
            .firstInList { it % 2 == 0 }
            .test().await().assertComplete().assertNoValues()

        Flowable.just(listOf(null, 1, 2))
            .firstInList { null != it && it % 2 == 0 }
            .test().await().assertComplete().assertValue(2)

        Flowable.just(listOf<Int?>(null, null))
            .firstInList { null != it && it % 2 == 0 }
            .test().await().assertComplete().assertNoValues()
    }

    companion object {
        private lateinit var mainThread: Thread
        private lateinit var ioThread: Thread
        private lateinit var singleThread: Thread

        @JvmStatic
        @BeforeClass
        fun before() {
            val latch = CountDownLatch(3)

            Schedulers.single().scheduleDirect {
                singleThread = Thread.currentThread()
                latch.countDown()
            }

            Schedulers.io().scheduleDirect {
                ioThread = Thread.currentThread()
                latch.countDown()
            }

            AndroidSchedulers.mainThread().scheduleDirect {
                mainThread = Thread.currentThread()
                latch.countDown()
            }

            latch.await()

        }
    }

    interface ITestEvent
    object TestEvent01 : ITestEvent
    object TestEvent02 : ITestEvent
    object TestEvent03 : ITestEvent
}
