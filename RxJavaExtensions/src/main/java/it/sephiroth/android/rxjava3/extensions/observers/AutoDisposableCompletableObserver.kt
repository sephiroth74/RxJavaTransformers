/*
 * MIT License
 *
 * Copyright (c) 2021 Alessandro Crugnola
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package it.sephiroth.android.rxjava3.extensions.observers

/**
 * Auto disposable Completable Observer
 * Upon completion, or error, will automatically unsubscribe from the
 * upstream
 *
 * @author Alessandro Crugnola on 06.01.21 - 13:23
 */
@Suppress("unused")
class AutoDisposableCompletableObserver() : DisposableCompletableObserver() {

    private var _doOnComplete: (() -> Unit)? = null
    private var _doOnError: ((Throwable) -> Unit)? = null
    private var _doOnStart: (() -> Unit)? = null
    private var _doOnDispose: (() -> Unit)? = null

    constructor(builder: (AutoDisposableCompletableObserver.() -> Unit)) : this() {
        this.builder()
    }

    override fun onComplete() {
        _doOnComplete?.invoke()
        dispose()
    }

    override fun onError(e: Throwable) {
        _doOnError?.invoke(e)
        dispose()
    }

    override fun onStart() {
        _doOnStart?.invoke()
    }

    override fun onDispose() {
        _doOnDispose?.invoke()
        clear()
    }

    fun doOnComplete(t: (() -> Unit)) {
        _doOnComplete = t
    }

    fun doOnError(t: ((Throwable) -> Unit)) {
        _doOnError = t
    }

    fun doOnStart(t: (() -> Unit)) {
        _doOnStart = t
    }

    fun doOnDispose(t: (() -> Unit)) {
        _doOnDispose = t
    }

    private fun clear() {
        println("clear")
        _doOnComplete = null
        _doOnError = null
        _doOnStart = null
        _doOnDispose = null
    }
}
