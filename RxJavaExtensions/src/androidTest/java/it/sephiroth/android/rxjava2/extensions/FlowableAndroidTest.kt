package it.sephiroth.android.rxjava2.extensions

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import it.sephiroth.android.rxjava2.extensions.flowable.pingPong
import it.sephiroth.android.rxjava2.extensions.flowable.skipBetween
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 01.03.21 - 10:13
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
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


    interface ITestEvent
    object TestEvent01 : ITestEvent
    object TestEvent02 : ITestEvent
    object TestEvent03 : ITestEvent
}
