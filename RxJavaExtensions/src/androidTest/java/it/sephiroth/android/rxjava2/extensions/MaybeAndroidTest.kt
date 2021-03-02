package it.sephiroth.android.rxjava2.extensions

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import io.reactivex.Maybe
import io.reactivex.Single
import it.sephiroth.android.rxjava2.extensions.maybe.autoSubscribe
import it.sephiroth.android.rxjava2.extensions.single.autoSubscribe
import it.sephiroth.android.rxjava2.extensions.single.firstInList
import it.sephiroth.android.rxjava2.extensions.single.mapList
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicIntegerArray
import java.util.concurrent.atomic.AtomicReferenceArray


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 02.03.21 - 16:50
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class MaybeAndroidTest {
    @Test
    fun test01() {

        val result = mutableListOf<String>()
        val countDownLatch = CountDownLatch(1)

        Maybe.just(1).autoSubscribe {
            doOnStart { result.add("start") }
            doOnSuccess { result.add("success") }
            doOnComplete { result.add("complete") }
            doOnError { result.add("error") }
            doOnFinish {
                result.add("finish")
                countDownLatch.countDown()
            }
        }

        countDownLatch.await(1, TimeUnit.SECONDS)
        Assert.assertEquals(0, countDownLatch.count)
        Assert.assertEquals(result, mutableListOf("start", "success", "finish"))
    }

    @Test
    fun test02() {

        val result = mutableListOf<String>()
        val countDownLatch = CountDownLatch(1)

        Maybe.empty<Int>().autoSubscribe {
            doOnStart { result.add("start") }
            doOnSuccess { result.add("success") }
            doOnComplete { result.add("complete") }
            doOnError { result.add("error") }
            doOnFinish {
                result.add("finish")
                countDownLatch.countDown()
            }
        }

        countDownLatch.await(1, TimeUnit.SECONDS)
        Assert.assertEquals(0, countDownLatch.count)
        Assert.assertEquals(result, mutableListOf("start", "complete", "finish"))
    }

    @Test
    fun test03() {
        val result = mutableListOf<String>()
        val countDownLatch = CountDownLatch(1)

        Maybe.error<Int>(IllegalStateException("test exception")).autoSubscribe {
            doOnStart { result.add("start") }
            doOnSuccess { result.add("success") }
            doOnComplete { result.add("complete") }
            doOnError { result.add("error") }
            doOnFinish {
                result.add("finish")
                countDownLatch.countDown()
            }
        }

        countDownLatch.await(1, TimeUnit.SECONDS)
        Assert.assertEquals(0, countDownLatch.count)
        Assert.assertEquals(result, mutableListOf("start", "error", "finish"))
    }
}
