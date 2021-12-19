package it.sephiroth.android.rxjava3.extensions

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import it.sephiroth.android.rxjava3.extensions.context.observeBroadcasts
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
    fun test01() {
        val latch = CountDownLatch(1)
        val result = AtomicInteger(0)
        val context = InstrumentationRegistry.getInstrumentation().context
        val s = context
            .observeBroadcasts(Intent.ACTION_TIME_TICK)
            .observeMain()
            .subscribe {
                println("tick :$it")
                result.addAndGet(1)
                latch.countDown()
            }

        latch.await(1, TimeUnit.MINUTES)
        Assert.assertEquals(1, result.get())
    }

    @Test
    fun test02() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val latch = arrayOf(CountDownLatch(1))
        val result = AtomicInteger()
        val d = context
            .observeBroadcasts("test.ACTION_1")
            .observeMain()
            .subscribe {
                println("received: $it")
                result.addAndGet(1)
                latch[0].countDown()
            }

        context.sendBroadcast(Intent("test.ACTION_1"))
        latch[0].await(1, TimeUnit.SECONDS)
        Assert.assertEquals(1, result.get())

        // now disposing receiver. we should not receive another intent notification
        d.dispose()
        latch[0] = CountDownLatch(1)
        context.sendBroadcast(Intent("test.ACTION_1"))
        Assert.assertEquals(1, result.get())
    }
}
