package it.sephiroth.android.rxjava3.extensions

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.processors.PublishProcessor
import io.reactivex.rxjava3.schedulers.Schedulers
import it.sephiroth.android.rxjava3.extensions.operators.FlowableTransformers
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 01.03.21 - 10:53
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class FlowableTransformerAndroidTest {

    @Test
    fun test01() {
        val now = System.currentTimeMillis()

        val o1 = PublishProcessor.create<Boolean>({ emitter ->
            Thread.sleep(550)
            emitter.onNext(true)
            emitter.onNext(false)

            Thread.sleep(550)
            emitter.onNext(true)
            emitter.onNext(false)

            Thread.sleep(550)
            emitter.onNext(true)
            emitter.onNext(false)

            emitter.onComplete()
        }, BackpressureStrategy.BUFFER).subscribeOn(Schedulers.newThread())


        Flowable
            .create<Long>({ emitter ->
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
            }, BackpressureStrategy.BUFFER)
            .subscribeOn(Schedulers.newThread())
            .compose(FlowableTransformers.valveLast(o1, false))
            .doOnNext {
                println("Target: [${System.currentTimeMillis() - now}] received onNext($it)")
            }
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertValues(3, 6, 8)
            .assertValueCount(3)

        println("done")


    }
}
