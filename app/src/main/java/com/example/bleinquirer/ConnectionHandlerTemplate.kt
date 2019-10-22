package com.example.bleinquirer

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

sealed class Result
object Success : Result() {
    override fun toString() = "Success"
}

open class Error(val message: String) : Result() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Error) return false
        return message == other.message
    }

    override fun hashCode(): Int = message.hashCode()
    override fun toString() = "Error($message)"
}

object TimedOut : Error("timeout")
object Busy : Error("busy")

class ConnectionHandlerTemplate {

    private enum class ConnectionState {
        NOT_CONNECTED, CONNECTING, CONNECTED, DISCONNECTING
    }

    private enum class DiscoveryState {
        NOT_DISCOVERED, DISCOVERING, DISCOVERED
    }

    private enum class ReadingState {
        NOT_READ, READING, READ
    }

    private enum class Operation {
        NONE, CONNECTING, DISCOVERING, READING, DISCONNECTING
    }

    /*private enum class OperationState {
        NOT_STARTED, IN_PROGRESS, COMPLETED
    }

    private data class CurrentOperation(val operation: Operation, val state: OperationState, val result: Result)
    private val myCurrentOperation = AtomicReference(CurrentOperation(Operation.NONE, OperationState.NOT_STARTED, Busy))*/

    private val connectionState = AtomicReference(ConnectionState.NOT_CONNECTED)
    private val discoveryState = AtomicReference(DiscoveryState.NOT_DISCOVERED)
    private val readingState = AtomicReference(ReadingState.NOT_READ)

    private val currentOperation = AtomicReference(Operation.NONE)
    private val operationResult = AtomicReference<Result>(null)

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    private fun awaitCompletion(timeout: Timeout): Result =
        if (lock.withLock { condition.await(timeout.value, timeout.unit) })
            operationResult.get()
        else TimedOut

    private fun signalCompletion(result: Result) {
        operationResult.set(result)
        lock.withLock { condition.signalAll() }
    }

    fun handleConnectionRequest(timeout: Timeout): Result {
        if (!currentOperation.compareAndSet(Operation.NONE, Operation.CONNECTING)) {
            return Busy
        }

        try {
            if (connectionState.compareAndSet(
                    ConnectionState.NOT_CONNECTED,
                    ConnectionState.CONNECTING
                )
            ) {
                if (awaitCompletion(timeout) is TimedOut) {
                    connectionState.compareAndSet(
                        ConnectionState.CONNECTING,
                        ConnectionState.NOT_CONNECTED
                    )
                    return TimedOut
                }
            }

            return if (connectionState.get() == ConnectionState.CONNECTED) Success else operationResult.get()
        } finally {
            currentOperation.set(Operation.NONE)
        }
    }

    fun handleDisconnectionRequest(timeout: Timeout): Result {
        if (!currentOperation.compareAndSet(Operation.NONE, Operation.DISCONNECTING)) {
            return Busy
        }

        try {
            if (connectionState.compareAndSet(
                    ConnectionState.CONNECTED,
                    ConnectionState.DISCONNECTING
                )
            ) {
                return awaitCompletion(timeout)
            }
            return if (connectionState.get() == ConnectionState.NOT_CONNECTED) Success else operationResult.get()
        } finally {
            currentOperation.set(Operation.NONE)
        }
    }

    fun handleConnected() {
        if (connectionState.compareAndSet(ConnectionState.CONNECTING, ConnectionState.CONNECTED)) {
            signalCompletion(Success)
        }
    }

    fun handleDisconnected(reason: Result) {
        connectionState.set(ConnectionState.NOT_CONNECTED)
        discoveryState.set(DiscoveryState.NOT_DISCOVERED)
        readingState.set(ReadingState.NOT_READ)
        signalCompletion(reason)
    }

    fun handleDiscoveryRequest(timeout: Timeout): Result {
        if (!currentOperation.compareAndSet(Operation.NONE, Operation.DISCOVERING)) {
            return Busy
        }

        try {
            if (connectionState.get() == ConnectionState.CONNECTED
                && discoveryState.compareAndSet(
                    DiscoveryState.NOT_DISCOVERED,
                    DiscoveryState.DISCOVERING
                )
                && awaitCompletion(timeout) is TimedOut
            ) {
                discoveryState.compareAndSet(
                    DiscoveryState.DISCOVERING,
                    DiscoveryState.NOT_DISCOVERED
                )
                return TimedOut
            }
            return if (discoveryState.get() == DiscoveryState.DISCOVERED) Success else operationResult.get()
        } finally {
            currentOperation.set(Operation.NONE)
        }
    }

    fun handleServicesDiscovered() {
        discoveryState.set(DiscoveryState.DISCOVERED)
        signalCompletion(Success)
    }

    fun handleServicesDiscoveryError(result: Result) {
        discoveryState.set(DiscoveryState.NOT_DISCOVERED)
        signalCompletion(result)
    }

    fun handleReadRequest(timeout: Timeout): Result {
        if (!currentOperation.compareAndSet(Operation.NONE, Operation.READING)) {
            return Busy
        }

        try {
            if (connectionState.get() == ConnectionState.CONNECTED
                && discoveryState.get() == DiscoveryState.DISCOVERED
                && readingState.compareAndSet(ReadingState.NOT_READ, ReadingState.READING)
                && awaitCompletion(timeout) is TimedOut
            ) {
                readingState.compareAndSet(ReadingState.READING, ReadingState.NOT_READ)
                return TimedOut
            }
            return if (readingState.get() == ReadingState.READ) Success else operationResult.get()
        } finally {
            currentOperation.set(Operation.NONE)
        }
    }

    fun handleReadingSuccess() {
        readingState.set(ReadingState.READ)
        signalCompletion(Success)
    }

    fun handleReadingError(result: Result) {
        readingState.set(ReadingState.READ)
        signalCompletion(result)
    }
}
