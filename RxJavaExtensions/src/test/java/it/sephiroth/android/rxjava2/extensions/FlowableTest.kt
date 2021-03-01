package it.sephiroth.android.rxjava2.extensions

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import it.sephiroth.android.rxjava2.extensions.flowable.skipBetween
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 01.03.21 - 10:13
 */
class FlowableTest {
    @Test
    fun test01() {
        val now = System.currentTimeMillis()
        val total = AtomicInteger(0)

        Observable.create<Int> { emitter ->
            while (true) {
                Thread.sleep(10)

                emitter.onNext(1)

                if (System.currentTimeMillis() - now > 2000) {
                    println("[${System.currentTimeMillis() - now}] emitting onComplete...")
                    emitter.onComplete()
                    break
                }
            }
        }.toFlowable(BackpressureStrategy.BUFFER)
            .skipBetween(500, TimeUnit.MILLISECONDS)
            .doOnNext {
                println("[${System.currentTimeMillis() - now}] received onNext($it)")
                total.set(total.get() + it)
            }
            .test()
            .awaitDone(3, TimeUnit.SECONDS)
            .assertComplete()

        val totalTime = System.currentTimeMillis() - now
        println("totalTime: $totalTime")

        Assert.assertEquals(3, total.get())
        Assert.assertTrue(totalTime > 500)
    }
}
