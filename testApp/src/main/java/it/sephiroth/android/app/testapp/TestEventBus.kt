package it.sephiroth.android.app.testapp

import java.util.*


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 16.12.21 - 16:11
 */
class TestEventBus : EventBus<TestEventBus.TestEvent>(TestEventBus::class.java.simpleName) {

    companion object {
        @JvmStatic
        val instance = TestEventBus()
    }

    abstract class TestEvent : Event

    data class TestEvent01(val name: String) : TestEvent() {
        val value = "${Date()} - $name"
    }

    data class TestEvent02(val value: Int) : TestEvent()
}
