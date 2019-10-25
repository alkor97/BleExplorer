package com.example.bleinquirer.handler.impl

import com.example.bleinquirer.Timeout
import com.example.bleinquirer.handler.*

class RequesterImpl(
    private val start: (Operation.Type, () -> Result) -> Result,
    private val isConnected: () -> Boolean,
    private val isDiscovered: () -> Boolean,
    private val awaitCompletion: (Timeout) -> Result
) : TemplateHandler.Requester {

    override fun handleConnectionRequest(timeout: Timeout, block: () -> Result): Result =
        start(Operation.Type.CONNECT) {
            var result: Result =
                Success
            if (!isConnected()) {
                result = block()
                if (result is Success) {
                    result = awaitCompletion(timeout)
                }
            }
            return@start if (isConnected()) Success else result
        }

    override fun handleDisconnectionRequest(
        timeout: Timeout,
        block: () -> Result
    ): Result = start(Operation.Type.DISCONNECT) {
        var result: Result =
            Success
        if (isConnected()) {
            result = block()
            if (result is Success) {
                result = awaitCompletion(timeout)
            }
        }
        return@start if (!isConnected()) Success else result
    }

    override fun handleDiscoveryRequest(timeout: Timeout, block: () -> Result): Result =
        start(Operation.Type.DISCOVER) {
            if (!isConnected())
                return@start NotConnected

            if (isDiscovered())
                return@start Success

            var result: Result = block()
            if (result !is Success)
                return@start result

            result = awaitCompletion(timeout)
            return@start if (isDiscovered()) Success else result
        }

    override fun handleReadRequest(timeout: Timeout, block: () -> Result): Result =
        start(Operation.Type.READ) {
            if (!isConnected())
                return@start NotConnected

            if (!isDiscovered())
                return@start NotDiscovered

            val result = block()
            if (result !is Success)
                return@start result

            return@start awaitCompletion(timeout)
        }
}