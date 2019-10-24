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
object NotConnected : Error("not connected")
object NotDiscovered : Error("not discovered")

class ConnectionHandlerTemplate {

    private enum class ConnectionState {
        NOT_CONNECTED, CONNECTING, CONNECTED, DISCONNECTING
    }

    private enum class DiscoveryState {
        NOT_DISCOVERED, DISCOVERING, DISCOVERED
    }

    private enum class ReadingState {
        READING, READ
    }

    private enum class Operation {
        NONE, CONNECT, DISCOVER, READ, DISCONNECT
    }

    private val connectionState = AtomicReference(ConnectionState.NOT_CONNECTED)
    private val discoveryState = AtomicReference(DiscoveryState.NOT_DISCOVERED)
    private val readingState = AtomicReference(ReadingState.READ)

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

    private fun tryStartOperation(operation: Operation) =
        currentOperation.compareAndSet(Operation.NONE, operation)

    private fun isConnected() = connectionState.get() == ConnectionState.CONNECTED

    private fun tryStartConnecting() =
        connectionState.compareAndSet(ConnectionState.NOT_CONNECTED, ConnectionState.CONNECTING)

    private fun tryStartDisconnecting() =
        connectionState.compareAndSet(ConnectionState.CONNECTED, ConnectionState.DISCONNECTING)

    private fun isDiscovered() = discoveryState.get() == DiscoveryState.DISCOVERED

    private fun tryStartDiscovering() =
        discoveryState.compareAndSet(DiscoveryState.NOT_DISCOVERED, DiscoveryState.DISCOVERING)

    private fun completeDiscovering(state: DiscoveryState) =
        discoveryState.compareAndSet(DiscoveryState.DISCOVERING, state)

    private fun tryStartReading() =
        readingState.compareAndSet(ReadingState.READ, ReadingState.READING)

    private fun isRead() = readingState.get() == ReadingState.READ

    private fun completeReading() =
        readingState.compareAndSet(ReadingState.READING, ReadingState.READ)

    fun handleConnectionRequest(timeout: Timeout): Result {
        if (!tryStartOperation(Operation.CONNECT))
            return Busy

        try {
            if (tryStartConnecting()) {
                if (awaitCompletion(timeout) is TimedOut) {
                    connectionState.compareAndSet(
                        ConnectionState.CONNECTING,
                        ConnectionState.NOT_CONNECTED
                    )
                    return TimedOut
                }
            }

            return if (isConnected()) Success else operationResult.get()
        } finally {
            currentOperation.set(Operation.NONE)
        }
    }

    fun handleDisconnectionRequest(timeout: Timeout): Result {
        if (!tryStartOperation(Operation.DISCONNECT))
            return Busy

        try {
            if (tryStartDisconnecting())
                return awaitCompletion(timeout)

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
        readingState.set(ReadingState.READ)
        signalCompletion(reason)
    }

    fun handleDiscoveryRequest(timeout: Timeout): Result {
        if (!tryStartOperation(Operation.DISCOVER))
            return Busy

        try {
            if (!isConnected())
                return NotConnected

            if (tryStartDiscovering() && awaitCompletion(timeout) is TimedOut) {
                completeDiscovering(DiscoveryState.NOT_DISCOVERED)
                return TimedOut
            }

            return if (isDiscovered()) Success else operationResult.get()
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
        if (!tryStartOperation(Operation.READ))
            return Busy

        try {
            if (!isConnected())
                return NotConnected

            if (!isDiscovered())
                return NotDiscovered

            if (tryStartReading() && awaitCompletion(timeout) is TimedOut) {
                completeReading()
                return TimedOut
            }
            return if (isRead()) Success else operationResult.get()
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
