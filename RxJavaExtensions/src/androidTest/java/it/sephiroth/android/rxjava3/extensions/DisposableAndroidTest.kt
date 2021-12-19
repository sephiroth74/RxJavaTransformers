package it.sephiroth.android.rxjava3.extensions

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import io.reactivex.rxjava3.disposables.Disposable
import it.sephiroth.android.rxjava3.extensions.disposable.disposeSafe
import it.sephiroth.android.rxjava3.extensions.disposable.isNullOrDisposed
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith


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
        val disposable: Disposable? = null
        Assert.assertTrue(disposable.isNullOrDisposed())
        Assert.assertFalse(Disposable.empty().isNullOrDisposed())
        Assert.assertTrue(Disposable.disposed().isNullOrDisposed())
    }

    @Test
    fun test02() {
        val nullDisposable: Disposable? = null
        nullDisposable.disposeSafe()
        Assert.assertTrue(nullDisposable.isNullOrDisposed())

        val d2 = Disposable.disposed()
        d2.disposeSafe()
        Assert.assertTrue(d2.isDisposed)

        val d3 = Disposable.empty()
        Assert.assertFalse(d3.isDisposed)
        d3.disposeSafe()
        Assert.assertTrue(d3.isDisposed)
    }
}
