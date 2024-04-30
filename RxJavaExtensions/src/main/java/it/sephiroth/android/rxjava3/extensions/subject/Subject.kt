package it.sephiroth.android.rxjava3.extensions.subject

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.Subject
import it.sephiroth.android.rxjava3.extensions.operators.ObservableTransformers

fun <T : Any> Subject<T>.doOnFirst(action: (T) -> Unit): Observable<T> =
    compose(ObservableTransformers.doOnFirst(action))

fun <T : Any> Subject<T>.doAfterFirst(action: (T) -> Unit): Observable<T> =
    compose(ObservableTransformers.doAfterFirst(action))

fun <T : Any> Subject<T>.doOnNth(nth: Long, action: (T) -> Unit): Observable<T> =
    compose(ObservableTransformers.doOnNth(nth, action))

fun <T : Any> Subject<T>.doAfterNth(nth: Long, action: (T) -> Unit): Observable<T> =
    compose(ObservableTransformers.doAfterNth(nth, action))
