package it.sephiroth.android.rxjava2.extensions

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import it.sephiroth.android.rxjava2.extensions.observabletransformers.ObservableTransformers
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 01.03.21 - 10:53
 */
class ObservableTransformerTest {
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
            .assertValueCount(3)
            .assertValues(3, 6, 8)


        println("done")


    }
}
