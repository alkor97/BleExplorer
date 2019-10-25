package com.example.bleinquirer.handler.impl

import com.example.bleinquirer.handler.Result
import com.example.bleinquirer.handler.Success
import com.example.bleinquirer.handler.TemplateHandler

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
