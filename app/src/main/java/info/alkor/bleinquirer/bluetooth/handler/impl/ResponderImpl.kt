package info.alkor.bleinquirer.bluetooth.handler.impl

import info.alkor.bleinquirer.bluetooth.handler.Result
import info.alkor.bleinquirer.bluetooth.handler.Success
import info.alkor.bleinquirer.bluetooth.handler.TemplateHandler

class ResponderImpl(
    private val setConnected: (Boolean) -> Unit,
    private val setDiscovered: (Boolean) -> Unit,
    private val signalCompletion: (Result) -> Unit
) : TemplateHandler.Responder {
    override fun handleConnected() {
        setConnected(true)
        signalCompletion(Success)
    }

    override fun handleDisconnected(reason: Result) {
        setConnected(false)
        setDiscovered(false)
        signalCompletion(reason)
    }

    override fun handleServicesDiscovered() {
        setDiscovered(true)
        signalCompletion(Success)
    }

    override fun handleServicesDiscoveryError(reason: Result) {
        setDiscovered(false)
        signalCompletion(reason)
    }

    override fun handleReadingSuccess() = signalCompletion(Success)
    override fun handleReadingError(reason: Result) = signalCompletion(reason)
}
