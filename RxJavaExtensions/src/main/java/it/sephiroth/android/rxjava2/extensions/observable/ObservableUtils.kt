package it.sephiroth.android.rxjava2.extensions.observable

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 24.11.21 - 13:56
 */
object ObservableUtils {

    /**
     * Starts a timer which will be report to [onTick] listener on each timer tick. <br />
     * [onComplete] is called once the timer ends. <br />
     * Both [onTick] and [onComplete] will be invoked from the main thread. <br />
     * For instance `ObservableUtils.countDown(0, 10, 1, TimeUnit.SECONDS, listener1, listener2)` will
     * invoke listener1 every 1 second for 10 times (from 0 to 10)
     *
     * @param start the timer start value
     * @param end the timer end value
     * @param step defines every how many steps the [onTick] should be invoked
     * @param unit TimeUnit
     */
    @JvmStatic
    fun countDown(
        start: Long,
        end: Long,
        step: Long,
        unit: TimeUnit,
        onTick: ((Long) -> Unit),
        onComplete: (() -> Unit)?
    ): Disposable {
        require(start != end) { "start != end required but it was $start, $end" }
        require(step <= (end - start).absoluteValue) { "step is bigger than the time span" }
        val reversed = start > end
        val beginning = if (reversed) end else start
        val ending = (if (reversed) start else end) + 1
        val total = ending - beginning

        return Observable.intervalRange(0, total, 0L, 1, unit)
            .filter { value ->
                value % step == 0L || value == ending
            }
            .map { value ->
                if (reversed) start - value else value
            }
            .observeMain()
            .subscribe(
                { value ->
                    onTick.invoke(value)

                    val completed = value == end
                    if (completed) onComplete?.invoke()
                },
                { /** empty **/ })
    }
}
