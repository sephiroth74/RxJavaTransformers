@file:Suppress("unused")

package it.sephiroth.android.rxjava2.extensions.observabletransformers

import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import io.reactivex.annotations.SchedulerSupport
import io.reactivex.internal.functions.ObjectHelper
import it.sephiroth.android.rxjava2.extensions.operators.ObservableLastValve


/**
 * @author Alessandro Crugnola on 05.01.21 - 23:00
 */
object ObservableTransformers {

    /**
     * Relays values until the other ObservableSource signals false and resumes if the other
     * ObservableSource signals true again, like closing and opening a valve and not losing
     * any items from the main source.
     * When resumed, only the last value emitted by the source observable, if any.
     * <p>Properties:
     * <ul>
     * <li>The operator starts with an open valve.</li>
     * <li>If the other ObservableSource completes, the sequence terminates with an {@code IllegalStateException}.</li>
     * <li>The operator doesn't run on any particular {@link io.reactivex.Scheduler Scheduler}.</li>
     * </ul>
     * @param <T> the value type of the main source
     * @param other the other source
     * @return the new ObservableTransformer instance
     * @throws NullPointerException if {@code other} is null
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    fun <T> valveLast(other: ObservableSource<Boolean>): ObservableTransformer<T, T> {
        return valveLast(other, true)
    }

    @SchedulerSupport(SchedulerSupport.NONE)
    fun <T> valveLast(other: ObservableSource<Boolean>, defaultOpen: Boolean = true): ObservableTransformer<T, T> {
        ObjectHelper.requireNonNull(other, "other is null")
        return ObservableLastValve(other, defaultOpen)
    }

}
