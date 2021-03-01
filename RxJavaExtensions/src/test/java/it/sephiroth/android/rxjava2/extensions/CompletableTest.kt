package it.sephiroth.android.rxjava2.extensions

import io.reactivex.Completable
import io.reactivex.Scheduler
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.disposables.Disposable
import io.reactivex.internal.schedulers.ExecutorScheduler.ExecutorWorker
import io.reactivex.plugins.RxJavaPlugins
import it.sephiroth.android.rxjava2.extensions.completable.autoSubscribe
import it.sephiroth.android.rxjava2.extensions.completable.delay
import it.sephiroth.android.rxjava2.extensions.observers.AutoDisposableCompletableObserver
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 01.03.21 - 09:53
 */
class CompletableTest {
    @Test
    fun test01() {
        val now = System.currentTimeMillis()
        val latch = CountDownLatch(1)
        delay(1, TimeUnit.SECONDS) {
            latch.countDown()
        }

        latch.await(2, TimeUnit.SECONDS)
        Assert.assertEquals(0, latch.count)
        println("totalTime: ${System.currentTimeMillis() - now}")
        Assert.assertTrue(System.currentTimeMillis() - now > 1000)
    }

    @Test
    fun test02() {
        val latch = CountDownLatch(2)
        Completable.create { it.onComplete() }
            .autoSubscribe {
                doOnStart { latch.countDown() }
                doOnComplete { latch.countDown() }
            }

        latch.await(1, TimeUnit.SECONDS)
        Assert.assertEquals(0, latch.count)
    }

    @Test
    fun test03() {
        val latch = CountDownLatch(2)
        Completable.create { it.onComplete() }
            .autoSubscribe(AutoDisposableCompletableObserver() {
                doOnStart {
                    latch.countDown()
                }
                doOnComplete {
                    latch.countDown()
                }
            })

        latch.await(1, TimeUnit.SECONDS)
        Assert.assertEquals(0, latch.count)
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUpRxSchedulers() {
            val immediate: Scheduler = object : Scheduler() {
                override fun scheduleDirect(
                    run: Runnable,
                    delay: Long,
                    unit: TimeUnit
                ): Disposable {
                    Thread.sleep(unit.toMillis(delay))
                    return super.scheduleDirect(run, 0, unit)
                }

                override fun createWorker(): Worker {
                    return ExecutorWorker({ obj: Runnable -> obj.run() }, true)
                }
            }
            RxJavaPlugins.setInitIoSchedulerHandler { immediate }
            RxJavaPlugins.setInitComputationSchedulerHandler { immediate }
            RxJavaPlugins.setInitNewThreadSchedulerHandler { immediate }
            RxJavaPlugins.setInitSingleSchedulerHandler { immediate }
            RxAndroidPlugins.setInitMainThreadSchedulerHandler { immediate }
        }
    }
}
