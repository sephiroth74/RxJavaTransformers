package it.sephiroth.android.rxjava2.extensions

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import it.sephiroth.android.rxjava2.extensions.flowable.pong
import it.sephiroth.android.rxjava2.extensions.flowable.skipBetween
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 01.03.21 - 10:13
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class FlowableAndroidTest {
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

    fun test02() {
        val subject = BehaviorSubject.create<TestEvent>()

        subject.retry()
            .toFlowable(BackpressureStrategy.BUFFER)
            .pong(TestEventImpl2::class.java, TestEventImpl4::class.java)
            .doOnSubscribe {
                System.out.println("[FlowableTest] OnSubscribe")
            }
            .doOnNext { it ->
                System.out.println("[FlowableTest] onNext = $it")
            }
            .subscribe()


        subject.onNext(TestEventImpl1())
        subject.onNext(TestEventImpl2())
        subject.onNext(TestEventImpl2())
        subject.onNext(TestEventImpl3())
        subject.onNext(TestEventImpl4())
        subject.onNext(TestEventImpl4())
        subject.onNext(TestEventImpl1())
        subject.onNext(TestEventImpl2())
        subject.onNext(TestEventImpl3())
        subject.onNext(TestEventImpl4())
        subject.onComplete()

    }

    interface TestEvent
    class TestEventImpl1 : TestEvent
    class TestEventImpl2 : TestEvent
    class TestEventImpl3 : TestEvent
    class TestEventImpl4 : TestEvent
}
