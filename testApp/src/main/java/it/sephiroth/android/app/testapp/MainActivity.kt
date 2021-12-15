package it.sephiroth.android.app.testapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Flowable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.processors.PublishProcessor
import io.reactivex.subjects.BehaviorSubject
import it.sephiroth.android.rxjava2.extensions.flowable.prioritize
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

        val subject: Flowable<Boolean> = PublishProcessor.create<Boolean>().toSerialized().onBackpressureBuffer()
        val prioritized = subject.prioritize()

        val s10 = prioritized.prioritySubscribe(10, MySubscriber(10))
        val s5 = prioritized.prioritySubscribe(6, MySubscriber(6))
        val s1 = prioritized.prioritySubscribe(6, MySubscriber(6))
        val s0 = prioritized.prioritySubscribe(0, MySubscriber(0))
        val s8 = prioritized.prioritySubscribe(8, MySubscriber(8))
        val s11 = prioritized.prioritySubscribe(11, MySubscriber(11))
        prioritized.subscribe(MySubscriber(12))

        prioritized.onNext(true)
        s1.dispose()
        s0.dispose()
        s8.dispose()
        val sNeg = prioritized.prioritySubscribe(-1, MySubscriber(-1))
        prioritized.onNext(false)

        sNeg.dispose()
        s11.dispose()
        s5.dispose()
        s10.dispose()
    }

    class MyObserver(val priority: Int) : Observer<Boolean> {
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

    class MySubscriber(val priority: Int) : Subscriber<Boolean> {
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
