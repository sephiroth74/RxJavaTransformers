package it.sephiroth.android.rxjava2.extensions

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import io.reactivex.Single
import it.sephiroth.android.rxjava2.extensions.single.autoSubscribe
import it.sephiroth.android.rxjava2.extensions.single.firstInList
import it.sephiroth.android.rxjava2.extensions.single.mapList
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 02.03.21 - 16:50
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class SingleAndroidTest {
    @Test
    fun test01() {
        Single.just(listOf(1, 2, 3, 4, 5)).firstInList().test().await().assertValue(1)
    }

    @Test
    fun test02() {
        Single.just(listOf(1, 2, 3, 4, 5)).mapList { it * it }.test().await()
            .assertValues(listOf(1, 4, 9, 16, 25))
    }

    @Test
    fun test03() {
        val countDownLatch = CountDownLatch(2)
        Single.just(1).autoSubscribe {
            doOnSuccess { countDownLatch.countDown() }
            doOnStart { countDownLatch.countDown() }
        }

        countDownLatch.await(1, TimeUnit.SECONDS)
        Assert.assertEquals(0, countDownLatch.count)
    }
}
