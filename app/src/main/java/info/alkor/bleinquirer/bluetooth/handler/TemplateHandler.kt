package info.alkor.bleinquirer.bluetooth.handler

import info.alkor.bleinquirer.bluetooth.handler.impl.*
import info.alkor.bleinquirer.utils.Timeout

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

class TemplateHandler {

    interface Requester {
        fun handleConnectionRequest(timeout: Timeout, block: () -> Result = { Success }): Result
        fun handleDisconnectionRequest(
            timeout: Timeout,
            block: () -> Result = { Success }
        ): Result

        fun handleDiscoveryRequest(timeout: Timeout, block: () -> Result = { Success }): Result
        fun handleReadRequest(timeout: Timeout, block: () -> Result = { Success }): Result
    }

    interface Responder {
        fun handleConnected()
        fun handleDisconnected(reason: Result)
        fun handleServicesDiscovered()
        fun handleServicesDiscoveryError(reason: Result)
        fun handleReadingSuccess()
        fun handleReadingError(reason: Result)
    }

    private val connected = BinaryState()
    private val discovered = BinaryState()
    private val timeoutHandler = TimeoutHandler(Busy)
    private val operation = Operation()

    val requester: Requester = RequesterImpl(
        operation.guard(), connected.reader(), discovered.reader(), timeoutHandler.reader()
    )
    val responder: Responder =
        ResponderImpl(connected.writer(), discovered.writer(), timeoutHandler.writer())
}