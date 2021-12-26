package it.sephiroth.android.rxjava3.extensions

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import it.sephiroth.android.rxjava3.extensions.observable.autoSubscribe
import it.sephiroth.android.rxjava3.extensions.operators.ObservableTransformers
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 01.03.21 - 10:53
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ObservableTransformerAndroidTest {
    @Test
    fun test01() {

        val now = System.currentTimeMillis()

        val o1 = Observable.create<Boolean> { emitter ->
            Thread.sleep(550)
            emitter.onNext(true)
            emitter.onNext(false)

            Thread.sleep(550)
            emitter.onNext(true)
            emitter.onNext(false)

            Thread.sleep(550)
            emitter.onNext(true)
            emitter.onNext(false)

            Thread.sleep(550)
            emitter.onComplete()
        }.subscribeOn(Schedulers.newThread())


        Observable
            .create<Long> { emitter ->
                emitter.onNext(1)
                emitter.onNext(2)

                Thread.sleep(500)
                emitter.onNext(3)

                Thread.sleep(500)
                emitter.onNext(4)
                emitter.onNext(5)
                emitter.onNext(6)

                Thread.sleep(500)
                emitter.onNext(7)
                emitter.onNext(8)

                emitter.onComplete()
            }
            .subscribeOn(Schedulers.newThread())
            .compose(ObservableTransformers.valveLast(o1, false))
            .doOnNext {
                println("Target: [${System.currentTimeMillis() - now}] received onNext($it)")
            }
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertValues(3, 6, 8)
            .assertValueCount(3)

    }

    @Test
    fun test02() {
        val o1 = Observable.just(1, 2, 3, 4, 5)
        val o2 = PublishSubject.create<Boolean>()
        o1.compose(ObservableTransformers.valveLast(o2)).test().await().assertComplete().assertValues(1, 2, 3, 4, 5)
    }

    @Test
    fun test03() {
        val count = AtomicInteger()
        val latch = CountDownLatch(1)
        val o1 = Observable.create<Int> { emitter ->
            while (!emitter.isDisposed) emitter.onNext(count.getAndIncrement())
        }

        val o2 = PublishSubject.create<Boolean>()

        val composed = o1.compose(ObservableTransformers.valveLast(o2))
        composed
            .observeOn(Schedulers.computation())
            .autoSubscribe {
                doOnNext {
                    println("onNext($it)")
                    if (it > 0) {
                        dispose()
                        latch.countDown()
                    }
                }

                doOnComplete {
                    println("onComplete()")
                }
            }

        latch.await()
    }

    @Test
    fun test04() {
        val count = AtomicInteger()
        val o1 = Observable.create<Int> {
            while (!it.isDisposed) it.onNext(count.getAndIncrement())
        }
        val o2 = PublishSubject.create<Boolean>()

        val composed = o1.subscribeOn(Schedulers.computation()).compose(ObservableTransformers.valveLast(o2))
        composed
            .observeOn(Schedulers.computation())
            .doOnNext {
                println("test::onNext($it)")
                if (it == 1) {
                    o2.onComplete()
                }
            }.doOnError {
                println("test::onError($it)")

            }.test().await().assertError(IllegalStateException::class.java).assertNotComplete()

    }
}
