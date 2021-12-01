package it.sephiroth.android.app.testapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.subjects.BehaviorSubject
import it.sephiroth.android.rxjava2.extensions.observable.ObservableUtils
import it.sephiroth.android.rxjava2.extensions.observable.autoSubscribe
import it.sephiroth.android.rxjava2.extensions.observable.observeMain
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class MainActivity : AppCompatActivity() {

    private var disposable: Disposable = Disposables.disposed()
    private val pauseSubject: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)

    lateinit var textView: TextView
    lateinit var pauseButton: Button
    lateinit var startButton: Button

    var startTime = System.currentTimeMillis()
    var tickTime = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }

    override fun onContentChanged() {
        super.onContentChanged()

        textView = findViewById(R.id.textView)
        startButton = findViewById(R.id.button0)
        pauseButton = findViewById(R.id.button1)

        pauseButton.setOnClickListener {
            if (pauseSubject.value == true)
                Log.v(TAG, "resumed")
            else
                Log.v(TAG, "paused")
            pauseSubject.onNext(!pauseSubject.value!!)
        }

        pauseSubject.observeMain().autoSubscribe {
            doOnNext {
                if (it) pauseButton.text = "Resume" else pauseButton.text = "Pause"
            }
        }

        startButton.setOnClickListener { doTest() }
    }

    private fun tick(): Long {
        val elapsed = System.currentTimeMillis() - tickTime
        tickTime = System.currentTimeMillis()
        return elapsed
    }

    private fun doTest() {
        disposable.dispose()
        pauseSubject.onNext(false)

        startTime = System.currentTimeMillis()
        tickTime = startTime

        Log.d(TAG, "[onStart] startTime=$startTime")

        textView.text = "Started at $startTime"


        disposable = ObservableUtils.pausedTimer(10, TimeUnit.SECONDS, pauseSubject)
            .observeMain()
            .autoSubscribe {

                doOnNext {
                    val tickValue = tick()
                    Log.d(TAG, "[onnext] = $tickValue")
                    textView.text = "Next. elapsed = $tickValue"

                }

                doOnComplete {
                    Log.d(TAG, "[completed]")
                    pauseSubject.onNext(false)
                }
            }

    }

    companion object {
        const val TAG = "MainActivity"
    }
}
