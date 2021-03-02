package it.sephiroth.android.rxjava2.extensions

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import it.sephiroth.android.rxjava2.extensions.disposable.disposeSafe
import it.sephiroth.android.rxjava2.extensions.disposable.isDisposed
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 02.03.21 - 18:04
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class DisposableAndroidTest {
    @Test
    fun test01() {
        var disposable: Disposable? = null
        Assert.assertTrue(disposable.isDisposed())

        disposable = Observable.just(1, 2, 3, 4, 5).subscribe()
        Thread.sleep(100)
        Assert.assertTrue(disposable.isDisposed())

        disposable.disposeSafe()
        Assert.assertTrue(disposable.isDisposed())

        // second observable

        disposable = Observable.interval(10, TimeUnit.MILLISECONDS).subscribe()
        Thread.sleep(100)
        Assert.assertFalse(disposable.isDisposed())

        disposable.disposeSafe()
        Assert.assertTrue(disposable.isDisposed())
    }
}
