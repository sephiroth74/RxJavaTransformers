package it.sephiroth.android.app.testapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import io.reactivex.BackpressureStrategy
import io.reactivex.subjects.BehaviorSubject
import it.sephiroth.android.rxjava2.extensions.completable.delay
import it.sephiroth.android.rxjava2.extensions.flowable.pong
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    val subject = BehaviorSubject.create<TestEvent>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        delay(2, TimeUnit.SECONDS) {
            doTest()
        }
    }

    private fun doTest() {
        Log.i("MainActivity", "doTest")
        subject.retry()
            .toFlowable(BackpressureStrategy.BUFFER)
            .pong(TestEventImpl2::class.java, TestEventImpl4::class.java)
            .doOnSubscribe {
                Log.d("FlowableTest", "OnSubscribe")
            }
            .doOnNext { it ->
                Log.v("FlowableTest", "onNext = $it")
            }
            .subscribe()


        subject.onNext(TestEventImpl1())
        subject.onNext(TestEventImpl2())
        subject.onNext(TestEventImpl2())
        subject.onNext(TestEventImpl3())
        subject.onNext(TestEventImpl4())
        subject.onNext(TestEventImpl4())
        subject.onNext(TestEventImpl1())
        subject.onNext(TestEventImpl2())
        subject.onNext(TestEventImpl3())
        subject.onNext(TestEventImpl4())
        subject.onNext(TestEventImpl4())
        subject.onNext(TestEventImpl4())
        subject.onNext(TestEventImpl4())
        subject.onNext(TestEventImpl4())
        subject.onNext(TestEventImpl4())
        subject.onComplete()
    }

    abstract class TestEvent {
        override fun toString(): String {
            return "${this::class.java.simpleName}"
        }
    }

    class TestEventImpl1 : TestEvent()
    class TestEventImpl2 : TestEvent()
    class TestEventImpl3 : TestEvent()
    class TestEventImpl4 : TestEvent()
}
