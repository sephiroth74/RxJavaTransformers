package it.sephiroth.android.rxjava3.extensions

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import it.sephiroth.android.rxjava3.extensions.completable.delay
import it.sephiroth.android.rxjava3.extensions.context.observeBroadcasts
import it.sephiroth.android.rxjava3.extensions.observable.autoSubscribe
import it.sephiroth.android.rxjava3.extensions.observable.observeMain
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 19.12.21 - 15:29
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class ContextAndroidTest {
    @Test
    fun test02() {
        val action = "it.sephiroth.android.rxjava3.extensions.test.ACTION_1"
        val context = InstrumentationRegistry.getInstrumentation().context
        val latch = CountDownLatch(1)
        val result = AtomicInteger(0)
        val d = context
            .observeBroadcasts(action)
            .observeMain()
            .autoSubscribe {
                doOnNext {
                    println("received: $it")
                    result.addAndGet(1)
                    latch.countDown()
                }

                doOnError {
                    println("broadcast error error: $it")
                }
            }

        delay(1_000, TimeUnit.MILLISECONDS) {
            println("now sending the broadcast action $action..")
            context.sendBroadcast(Intent(action))
        }

        latch.await(2, TimeUnit.SECONDS)
        Assert.assertEquals(1, result.get())

        // now disposing receiver.
        // we should not receive intent notifications anymore
        d.dispose()

        context.sendBroadcast(Intent("test.ACTION_1"))
        latch.await(200, TimeUnit.MILLISECONDS)
        Assert.assertEquals(1, result.get())
    }
}
