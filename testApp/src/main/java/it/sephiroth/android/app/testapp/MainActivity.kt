package it.sephiroth.android.app.testapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.plusAssign
import it.sephiroth.android.rxjava3.extensions.completable.delay
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import timber.log.Timber
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    lateinit var textView: TextView
    lateinit var pauseButton: Button
    lateinit var startButton: Button

    val eventBus = TestEventBus.instance

    val logger = Timber.tag("MainActivity")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }

    override fun onContentChanged() {
        super.onContentChanged()

        textView = findViewById(R.id.textView)
        startButton = findViewById(R.id.button0)
        pauseButton = findViewById(R.id.button1)

        startButton.setOnClickListener {
            logger.i("onClick(start)")
            doTest()
        }

        pauseButton.setOnClickListener {
            logger.i("onClick(pause)")
            Runtime.getRuntime().gc()
        }

        onPrepareTests()
    }

    private fun onPrepareTests() {
        logger.i("onPrepareTest()")
        eventBus.send(TestEventBus.TestEvent01("onPrepare"))
    }


    private fun doTest() {
        logger.i("doTest()")

        val disposables = CompositeDisposable()

        disposables += eventBus.listen<TestEventBus.TestEvent01> {
            logger.d("(9) received = $it")
        }

        disposables += eventBus.listen<TestEventBus.TestEvent02> {
            logger.d("(0) received = $it")
        }

        disposables += eventBus.listen<TestEventBus.TestEvent01> {
            logger.d("(1) received = $it")
        }

        delay(100, TimeUnit.MILLISECONDS) {
            eventBus.send(TestEventBus.TestEvent01("Primo"))
            eventBus.send(TestEventBus.TestEvent02(100))
        }
//
//        delay(500, TimeUnit.MILLISECONDS) {
//
//            disposables += eventBus.listen<TestEventBus.TestEvent01>(3) {
//                logger.d("(3) received = $it")
//            }
//
//            disposables += eventBus.listen<TestEventBus.TestEvent02>(3) {
//                logger.d("(3) received = $it")
//            }
//
//            eventBus.send(TestEventBus.TestEvent01("Secondo"))
//        }

        delay(1, TimeUnit.SECONDS) {
            disposables.dispose()
//            eventBus.send(TestEventBus.TestEvent01("Terzo"))
        }
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
}
