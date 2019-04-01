/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.flux

import io.reactivex.Observer
import io.reactivex.annotations.CheckReturnValue
import io.reactivex.annotations.NonNull
import io.reactivex.annotations.Nullable
import io.reactivex.disposables.Disposable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.subjects.Subject
import java.util.Stack
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * A ReplaySubject that emits the top most item on subscription.
 *
 * It also contains methods that manipulate the stack, but these do not affect any subscribed observers.
 *
 * For provenance, this was largely derived from PublishSubject, converted to Kotlin and a history stack added.
 */
class StackReplaySubject<T>
internal constructor() : Subject<T>() {

    /** The array of currently subscribed subscribers.  */
    private val subscribers: AtomicReference<Array<StackDisposable<T>>>

    /** The error, write before terminating and read after checking subscribers.  */
    internal var error: Throwable? = null

    private val history = Stack<T>()

    /** The terminated indicator for the subscribers array.  */
    private val terminated = emptyArray<StackDisposable<T>>()
    /** An empty subscribers array to avoid allocating it all the time.  */
    private val empty = emptyArray<StackDisposable<T>>()

    init {
        subscribers = AtomicReference(empty)
    }

    override fun subscribeActual(t: Observer<in T>) {
        val ps = StackDisposable(t, this)
        t.onSubscribe(ps)
        if (add(ps)) {
            // if cancellation happened while a successful add, the remove() didn't work
            // so we need to do it again
            if (ps.isDisposed) {
                remove(ps)
            } else {
                replay(ps)
            }
        } else {
            val ex = error
            if (ex != null) {
                t.onError(ex)
            } else {
                t.onComplete()
            }
        }
    }

    /**
     * Tries to add the given subscriber to the subscribers array atomically
     * or returns false if the subject has terminated.
     * @param ps the subscriber to add
     * @return true if successful, false if the subject has terminated
     */
    internal fun add(ps: StackDisposable<T>): Boolean {
        while (true) {
            val a = subscribers.get()
            if (a === terminated) {
                return false
            }

            val b = a + arrayOf(ps)

            if (subscribers.compareAndSet(a, b)) {
                return true
            }
        }
    }

    /**
     * Atomically removes the given subscriber if it is subscribed to the subject.
     * @param ps the subject to remove
     */
    internal fun remove(ps: StackDisposable<T>) {
        while (true) {
            val a = subscribers.get()
            if (a === terminated || a === empty) {
                return
            }

            val c = a.filterNot { it === ps }.toTypedArray()
            if (subscribers.compareAndSet(a, if (c.isNotEmpty()) c else empty)) {
                return
            }
        }
    }

    override fun onSubscribe(d: Disposable) {
        if (subscribers.get() === terminated) {
            d.dispose()
        }
    }

    override fun onNext(t: T) {
        synchronized(history) {
            history.push(t)
        }
        for (pd in subscribers.get()) {
            pd.onNext(t)
        }
    }

    override fun onError(t: Throwable) {
        if (subscribers.get() === terminated) {
            RxJavaPlugins.onError(t)
            return
        }
        error = t

        for (pd in subscribers.getAndSet(terminated)) {
            pd.onError(t)
        }
    }

    override fun onComplete() {
        if (subscribers.get() === terminated) {
            return
        }
        for (pd in subscribers.getAndSet(terminated)) {
            pd.onComplete()
        }
    }

    override fun hasObservers(): Boolean {
        return subscribers.get().isNotEmpty()
    }

    @Nullable
    override fun getThrowable(): Throwable? {
        return if (subscribers.get() === terminated) {
            error
        } else {
            null
        }
    }

    override fun hasThrowable(): Boolean {
        return subscribers.get() === terminated && error != null
    }

    override fun hasComplete(): Boolean {
        return subscribers.get() === terminated && error == null
    }

    /**
     * Replay the item on the top of the stack currently.
     */
    private fun replay(ps: StackDisposable<T>) {
        val item: T?
        synchronized(history) {
            item = if (history.isNotEmpty()) {
                history.peek()
            } else {
                null
            }
        }
        if (item != null) {
            ps.onNext(item)
        }
    }

    // Some utility methods to manipulate the stack.

    fun getValue() = synchronized(history) { if (history.isNotEmpty()) { history.peek() } else { null } }

    fun pop() = synchronized(history) { if (history.isNotEmpty()) { history.pop() } else { null } }

    /**
     * Only pop if the history will not empty after the pop.
     */
    fun safePop() {
        synchronized(history) {
            if (history.size > 1) {
                history.pop()
                assert(history.isNotEmpty())
            }
        }
    }

    fun trim(vararg values: T) {
        trim { values.contains(it) }
    }

    fun trim(test: (T) -> Boolean) {
        synchronized(history) {
            while (history.isNotEmpty() && test(history.peek())) {
                history.pop()
            }
        }
    }

    fun clear() = synchronized(history) { history.clear() }

    /**
     * Clear the history, except for the last thing added.
     */
    fun trimTail() {
        synchronized(history) {
            if (history.size <= 1) {
                return
            }
            val head = history.peek()
            history.clear()
            history.push(head)
        }
    }

    fun getSize() = synchronized(history) { history.size }

    /**
     * Wraps the actual subscriber, tracks its requests and makes cancellation
     * to remove itself from the current subscribers array.
     *
     * @param <T> the value type
    </T> */
    internal class StackDisposable<T>
    /**
     * Constructs a PublishSubscriber, wraps the actual subscriber and the state.
     * @param actual the actual subscriber
     * @param parent the parent PublishProcessor
     */(
         /** The actual subscriber.  */
         val downstream: Observer<in T>,
         /** The subject state.  */
         val parent: StackReplaySubject<T>
     ) : AtomicBoolean(), Disposable {

        fun onNext(t: T) {
            if (!get()) {
                downstream.onNext(t)
            }
        }

        fun onError(t: Throwable) {
            if (get()) {
                RxJavaPlugins.onError(t)
            } else {
                downstream.onError(t)
            }
        }

        fun onComplete() {
            if (!get()) {
                downstream.onComplete()
            }
        }

        override fun dispose() {
            if (compareAndSet(false, true)) {
                parent.remove(this)
            }
        }

        override fun isDisposed(): Boolean {
            return get()
        }
    }

    companion object {
        /**
         * Constructs a StackReplaySubject.
         * @param <T> the value type
         * @return the new StackReplaySubject
        </T> */
        @CheckReturnValue
        @NonNull
        fun <T> create(): StackReplaySubject<T> {
            return StackReplaySubject()
        }
    }
}