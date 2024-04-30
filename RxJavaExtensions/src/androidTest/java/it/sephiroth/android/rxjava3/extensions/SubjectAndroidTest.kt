package it.sephiroth.android.rxjava3.extensions

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableOperator
import io.reactivex.rxjava3.core.ObservableSource
import io.reactivex.rxjava3.core.ObservableTransformer
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import it.sephiroth.android.rxjava3.extensions.completable.delay
import it.sephiroth.android.rxjava3.extensions.observable.*
import it.sephiroth.android.rxjava3.extensions.observers.AutoDisposableObserver
import it.sephiroth.android.rxjava3.extensions.operators.ObservableTransformers
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 02.03.21 - 13:47
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SubjectAndroidTest {


    @Test
    fun test001() {
        val now = System.currentTimeMillis()
        val finalTime = AtomicLong()

        val first = AtomicLong()
        val afterFirst = AtomicLong()
        val afterFifth = AtomicLong()
        val nextCount = AtomicLong()
        val fifth = AtomicLong()

        val subject = BehaviorSubject.create<Int>() {
            it.onNext(1)
            it.onNext(2)
            it.onNext(3)
            it.onNext(4)
            it.onNext(5)
            it.onNext(6)
            it.onNext(7)
            it.onNext(8)
            it.onNext(9)
            it.onNext(10)
            it.onComplete()
        }

        subject
            .doOnFirst {
                first.incrementAndGet()
            }
            .doAfterFirst {
                afterFirst.incrementAndGet()
            }
            .doOnNth(5) {
                fifth.incrementAndGet()
            }
            .doAfterNth(5) {
                afterFifth.incrementAndGet()
            }
            .doOnNext {
                nextCount.incrementAndGet()
            }.doOnComplete {
                finalTime.set(System.currentTimeMillis() - now)
            }.doOnError {
                finalTime.set(System.currentTimeMillis() - now)
            }
            .test()
            .await()
            .assertComplete()

        Assert.assertEquals(10, nextCount.get())
        Assert.assertEquals(1, first.get())
        Assert.assertEquals(9, afterFirst.get())
        Assert.assertEquals(4, afterFifth.get())
        Assert.assertEquals(1, fifth.get())
    }

    companion object {
        private lateinit var ioThread: Thread
        private lateinit var singleThread: Thread

        @JvmStatic
        @BeforeClass
        fun before() {
            val latch = CountDownLatch(2)

            Schedulers.single().scheduleDirect {
                singleThread = Thread.currentThread()
                latch.countDown()
            }

            Schedulers.io().scheduleDirect {
                ioThread = Thread.currentThread()
                latch.countDown()
            }

            latch.await()

        }
    }
}
