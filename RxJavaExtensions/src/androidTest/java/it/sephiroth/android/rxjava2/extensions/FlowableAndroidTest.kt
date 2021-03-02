package it.sephiroth.android.rxjava2.extensions

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import it.sephiroth.android.rxjava2.extensions.flowable.skipBetween
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


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
}
