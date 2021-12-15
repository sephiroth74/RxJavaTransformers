package it.sephiroth.android.app.testapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Flowable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.functions.Consumer
import io.reactivex.processors.PublishProcessor
import io.reactivex.subjects.BehaviorSubject
import it.sephiroth.android.rxjava2.extensions.flowable.PrioritizedFlowable
import it.sephiroth.android.rxjava2.extensions.flowable.prioritize
import it.sephiroth.android.rxjava2.extensions.observable.PrioritizedObservable
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private var disposable: Disposable = Disposables.disposed()
    private val pauseSubject = BehaviorSubject.createDefault(false).toSerialized()

    lateinit var textView: TextView
    lateinit var pauseButton: Button
    lateinit var startButton: Button

    var startTime = System.currentTimeMillis()
    var tickTime = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.plant(Timber.DebugTree())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }

    override fun onContentChanged() {
        super.onContentChanged()

        textView = findViewById(R.id.textView)
        startButton = findViewById(R.id.button0)
        pauseButton = findViewById(R.id.button1)

        startButton.setOnClickListener {
            Timber.i("onClick(start)")
            doTest()
        }

        pauseButton.setOnClickListener {
            Timber.i("onClick(pause)")
            Runtime.getRuntime().gc()
        }
    }


    private fun doTest() {
        Timber.v("starting test")

        val prioritized: PrioritizedFlowable<Boolean> = PublishProcessor.create<Boolean>().toSerialized().onBackpressureBuffer().retry().prioritize()

        prioritized.prioritySubscribe(1) { Timber.d("Consumer::onNext(1)") }
        prioritized.prioritySubscribe(10) { Timber.d("Consumer::onNext(10)") }
        val s5 = prioritized.prioritySubscribe(5) { Timber.d("Consumer::onNext(5)") }

        prioritized.onNext(true)

        Timber.i("Adding new")
        s5.dispose()

        prioritized.prioritySubscribe(0) { Timber.d("Consumer::onNext(0)") }

        prioritized.onNext(false)
    }

    class MyObserver(private val priority: Int) : Observer<Boolean> {
        override fun onComplete() {
            Timber.d("[${priority}] onComplete()")
        }

        override fun onError(e: Throwable) {
            Timber.d("[${priority}] onError($e)")
        }

        override fun onNext(t: Boolean) {
            Timber.d("[${priority}] onNext($t)")
        }

        override fun onSubscribe(d: Disposable) {
            Timber.v("[${priority}] onSubscribe()")
        }
    }

    class MySubscriber(private val priority: Int) : Subscriber<Boolean> {
        override fun onComplete() {
            Timber.d("[${priority}] onComplete()")
        }

        override fun onError(e: Throwable) {
            Timber.d("[${priority}] onError($e)")
        }

        override fun onNext(t: Boolean) {
            Timber.d("[${priority}] onNext($t)")
        }

        override fun onSubscribe(s: Subscription?) {
            Timber.v("[${priority}] onSubscribe($s)")
        }

        protected fun finalize() {
            Timber.d("[${priority}] finalize()")
        }
    }

    companion object {
        const val TAG = "MainActivity"
    }
}
