package it.sephiroth.android.rxjava2.extensions.observable

import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 15.12.21 - 09:08
 */
class PrioritizedObservable<T>(s: Observable<T>) : Observable<T>(), Observer<T> {

    private var source = s.publish().refCount()
    private var intermediary = PublishSubject.create<T>().toSerialized()
    private val router = arrayListOf<Pair<Int, Subject<T>>>()

    init {
        source.subscribe(intermediary)
    }

    override fun subscribeActual(observer: Observer<in T>) {
        prioritySubscribe(1, observer)
    }

    override fun onSubscribe(d: Disposable) {
        router.forEach { it.second.onSubscribe(d) }
    }

    override fun onNext(t: T) {
        router.forEach { it.second.onNext(t) }
    }

    override fun onError(e: Throwable) {
        router.forEach { it.second.onError(e) }
    }

    override fun onComplete() {
        router.forEach { it.second.onComplete() }
    }

    fun prioritySubscribe(observer: Observer<in T>) = prioritySubscribe(PRIORITY_DEFAULT.value, observer)

    fun prioritySubscribe(priority: Int, obs: Observer<in T>): Disposable {
        val subject = PublishSubject.create<T>().toSerialized()
        val disposable = subject.autoSubscribe {
            doOnNext { obs.onNext(it) }
            doOnComplete { obs.onComplete() }
            doOnError { obs.onError(it) }
        }

        val entry = Pair(priority, subject)
        router.add(entry)
        router.sortBy { it.first }
        intermediary.subscribe(obs)

        return object : Disposable {
            override fun dispose() {
                disposable.dispose()
                router.remove(entry)
            }

            override fun isDisposed(): Boolean {
                return disposable.isDisposed
            }
        }
    }

    enum class Priority(val value: Int) {
        Low(10), Medium(5), High(1)
    }

    companion object {
        val PRIORITY_DEFAULT = Priority.Medium
    }
}

