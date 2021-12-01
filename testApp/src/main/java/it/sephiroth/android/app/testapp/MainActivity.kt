package it.sephiroth.android.app.testapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.subjects.BehaviorSubject
import it.sephiroth.android.rxjava2.extensions.flowable.observeMain
import it.sephiroth.android.rxjava2.extensions.observable.ObservableUtils
import it.sephiroth.android.rxjava2.extensions.observable.autoSubscribe
import it.sephiroth.android.rxjava2.extensions.observable.observeMain
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    val subject = BehaviorSubject.create<TestEvent>()
    val pauseSubject = BehaviorSubject.createDefault(false)

    lateinit var textView: TextView
    lateinit var pauseButton: Button
    lateinit var resumeButton: Button
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
        resumeButton = findViewById(R.id.button2)

        pauseButton.setOnClickListener {
            Log.v(TAG, "paused")
            pauseSubject.onNext(true)
        }
        resumeButton.setOnClickListener {
            Log.v(TAG, "resumed [${tick()}]")
            pauseSubject.onNext(false)
        }
        startButton.setOnClickListener { doTest() }
    }

    private fun elapsed() = System.currentTimeMillis() - startTime

    private fun tick(): Long {
        val elapsed = System.currentTimeMillis() - tickTime
        tickTime = System.currentTimeMillis()
        return elapsed
    }

    private fun doTest() {
        startTime = System.currentTimeMillis()
        tickTime = startTime

        Log.d(TAG, "[onStart] startTime=$startTime")

        textView.text = "Started at $startTime"

        ObservableUtils.pausedInterval(3, TimeUnit.SECONDS, pauseSubject)
            .observeMain()
            .autoSubscribe {

                doOnNext {
                    val currentTime = System.currentTimeMillis()
                    val tick = tick()
                    Log.d(TAG, "onNext value=$it, startTime=$startTime, tick=${tick}")
                    textView.setText("[onNext] Value=$it, tickTime=${tick}, elapsed=${elapsed()}")
                }

                doOnComplete {
                    Log.d(TAG, "[onComplete] tickTime=${tick()}, elapsed=${elapsed()}")
                }
            }
    }

    abstract class TestEvent {
        override fun toString(): String {
            return this::class.java.simpleName
        }
    }

    companion object {
        const val TAG = "MainActivity"
    }
}
