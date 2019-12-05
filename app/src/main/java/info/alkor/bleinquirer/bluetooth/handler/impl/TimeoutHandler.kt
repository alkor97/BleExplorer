package info.alkor.bleinquirer.bluetooth.handler.impl

import info.alkor.bleinquirer.bluetooth.handler.Result
import info.alkor.bleinquirer.bluetooth.handler.TimedOut
import info.alkor.bleinquirer.utils.Timeout
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TimeoutHandler(private val defaultResult: Result) {

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private val result = AtomicReference<Result>(defaultResult)

    private fun awaitCompletion(timeout: Timeout): Result =
        if (lock.withLock {
                result.set(defaultResult)
                condition.await(timeout.value, timeout.unit)
            })
            result.get()
        else TimedOut

    private fun signalCompletion(newResult: Result) {
        result.compareAndSet(defaultResult, newResult)
        lock.withLock { condition.signalAll() }
    }

    fun reader() = fun(timeout: Timeout) = awaitCompletion(timeout)
    fun writer() = fun(newResult: Result) { signalCompletion(newResult) }
}
