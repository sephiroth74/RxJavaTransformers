package it.sephiroth.android.rxjava3.extensions

import android.os.Looper
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import it.sephiroth.android.rxjava3.extensions.flowable.*
import it.sephiroth.android.rxjava3.extensions.observers.AutoDisposableSubscriber
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 01.03.21 - 10:13
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class FlowableAndroidTest {
    companion object {
        private const val TAG = "FlowableAndroidTest"
    }

    @Test
    fun test01() {
        val now = System.currentTimeMillis()

        Observable.create<Int> { emitter ->

            Thread.sleep(10)
            emitter.onNext(1) // valve is opened
            emitter.onNext(2)

            Thread.sleep(10)
            emitter.onNext(3)

            Thread.sleep(500)
            emitter.onNext(4) // valve is opened
            emitter.onNext(5)

            Thread.sleep(10)
            emitter.onNext(6)

            Thread.sleep(1000)
            emitter.onNext(7) // valve is opened
            emitter.onNext(8)

            emitter.onComplete()

        }.toFlowable(BackpressureStrategy.BUFFER)
            .skipBetween(500, TimeUnit.MILLISECONDS, true)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertComplete()
            .assertValues(1, 4, 7)

        val totalTime = System.currentTimeMillis() - now
        println("totalTime: $totalTime")

        Assert.assertTrue(totalTime > 500)
    }

    @Test
    fun test02() {
        Log.i(TAG, "test02")
        val subject = BehaviorSubject.create<ITestEvent>()
        val valueCount = CountDownLatch(5)
        val values = mutableListOf<ITestEvent>()

        subject.toFlowable(BackpressureStrategy.BUFFER)
            .pingPong(TestEvent01::class.java, TestEvent03::class.java)
            .doOnNext {
                println("onNext: $it")
                valueCount.countDown()
                values.add(it)
            }
            .doOnComplete {
                println("onComplete")
            }
            .test()
            .await(1, TimeUnit.SECONDS)

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

        valueCount.await(2, TimeUnit.SECONDS)
        Assert.assertEquals(0, valueCount.count)

        Assert.assertEquals(
            listOf(
                TestEvent01,
                TestEvent03,
                TestEvent01,
                TestEvent03,
                TestEvent01
            ), values
        )

        println(values)
    }

    @Test
    fun test03() {
        Flowable.just(1).debug("f1").debugWithThread("f1-thread").test().await()
        Flowable.error<Int>(RuntimeException("test")).debug("f2").debugWithThread("f2-thread").test().assertError(RuntimeException::class.java)
    }

    @Test
    fun test04() {
        val mainThread = Looper.getMainLooper().thread
        Flowable.just(1).observeMain().doOnNext {
            Assert.assertEquals(mainThread, Thread.currentThread())
        }.test().await()
    }


    @Test
    fun test05() {
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
        val flowable = Flowable.just(1, 2)
        flowable.subscribeOn(Schedulers.io())
            .observeOn(Schedulers.single())
            .autoSubscribe {
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
            }

        flowable.test().await().assertComplete()
        latch.await()
        Assert.assertEquals(listOf("start", "next", "next", "complete"), result)
    }


    @Test
    fun test06() {
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

        flowable.test().await().assertError(RuntimeException::class.java)
        latch.await()
        Assert.assertEquals(listOf("start", "error"), result)
    }

    interface ITestEvent
    object TestEvent01 : ITestEvent
    object TestEvent02 : ITestEvent
    object TestEvent03 : ITestEvent
}
