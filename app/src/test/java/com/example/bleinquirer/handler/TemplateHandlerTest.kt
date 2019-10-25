package com.example.bleinquirer.handler

import com.example.bleinquirer.Timeout
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TemplateHandlerTest {

    private val startTime = System.currentTimeMillis()
    private fun now() = (System.currentTimeMillis() - startTime)

    private fun reportTime(text: String) {
        System.out.printf("%04d:\t%s\n", now(), text)
    }

    companion object {
        val DEFAULT_TIMEOUT = Timeout(5)
        val DEFAULT_DELAY = Timeout(1)
        val SHORT_TIMEOUT = Timeout(1)
    }

    private fun runDelayed(
        name: String = "",
        delay: Timeout = DEFAULT_DELAY,
        procedure: () -> Unit = {}
    ) = GlobalScope.launch {
        delay(delay.unit.toMillis(delay.value))
        if (!name.isBlank()) {
            reportTime(name)
        }
        procedure()
    }

    private val instance = TemplateHandler()
    private val requester = instance.requester
    private val responder = instance.responder

    private fun postHandleConnected(delay: Timeout = DEFAULT_DELAY) =
        runDelayed("handleConnected", delay) { responder.handleConnected() }

    private fun callHandleConnectionRequest(timeout: Timeout = DEFAULT_TIMEOUT): Result {
        reportTime("handleConnectionRequest")
        return requester.handleConnectionRequest(timeout)
    }

    private fun postHandleServicesDiscovered(delay: Timeout = DEFAULT_DELAY) =
        runDelayed("handleServicesDiscovered", delay) { responder.handleServicesDiscovered() }

    private fun postHandleServicesDiscoveryError(result: Result, delay: Timeout = DEFAULT_DELAY) =
        runDelayed("handleServicesDiscoveryError", delay) {
            responder.handleServicesDiscoveryError(result)
        }

    private fun callHandleDiscoveryRequest(timeout: Timeout = DEFAULT_TIMEOUT): Result {
        reportTime("handleDiscoveryRequest")
        return requester.handleDiscoveryRequest(timeout)
    }

    private fun postHandleReadingSuccess(delay: Timeout = DEFAULT_DELAY) =
        runDelayed("handleReadingSuccess", delay) { responder.handleReadingSuccess() }

    private fun postHandleReadingError(result: Result, delay: Timeout = DEFAULT_DELAY) =
        runDelayed("handleReadingError", delay) { responder.handleReadingError(result) }

    private fun callHandleReadRequest(timeout: Timeout = DEFAULT_TIMEOUT): Result {
        reportTime("handleReadRequest")
        return requester.handleReadRequest(timeout)
    }

    private fun postHandleDisconnected(result: Result = Success, delay: Timeout = DEFAULT_DELAY) =
        runDelayed("handleDisconnected", delay) { responder.handleDisconnected(result) }

    private fun callHandleDisconnectionRequest(timeout: Timeout = DEFAULT_TIMEOUT): Result {
        reportTime("handleDisconnectionRequest")
        return requester.handleDisconnectionRequest(timeout)
    }

    @Test
    fun testHappyPath() {
        postHandleConnected()
        assertEquals(Success, callHandleConnectionRequest())

        postHandleServicesDiscovered()
        assertEquals(Success, callHandleDiscoveryRequest())

        postHandleReadingSuccess()
        assertEquals(Success, callHandleReadRequest())

        postHandleDisconnected()
        assertEquals(Success, callHandleDisconnectionRequest())
    }

    @Test
    fun testConnectionTimeout() {
        assertNotEquals(Success, callHandleConnectionRequest(SHORT_TIMEOUT))
    }

    @Test
    fun testServiceDiscoveryTimeout() {
        postHandleConnected()
        assertEquals(Success, callHandleConnectionRequest())

        assertEquals(TimedOut, callHandleDiscoveryRequest(SHORT_TIMEOUT))
    }

    @Test
    fun testServiceDiscoveryError() {
        postHandleConnected()
        assertEquals(Success, callHandleConnectionRequest())

        val expectedResult = Error("discovery error")
        postHandleServicesDiscoveryError(expectedResult)
        assertEquals(expectedResult, callHandleDiscoveryRequest())
    }

    @Test
    fun testReadingTimeout() {
        postHandleConnected()
        assertEquals(Success, callHandleConnectionRequest())

        postHandleServicesDiscovered()
        assertEquals(Success, callHandleDiscoveryRequest())

        postHandleReadingSuccess()
        assertEquals(Success, callHandleReadRequest())

        assertEquals(TimedOut, callHandleDisconnectionRequest(SHORT_TIMEOUT))
    }

    @Test
    fun testReadingError() {
        postHandleConnected()
        assertEquals(Success, callHandleConnectionRequest())

        postHandleServicesDiscovered()
        assertEquals(Success, callHandleDiscoveryRequest())

        val expectedResult = Error("discovery error")
        postHandleReadingError(expectedResult)
        assertEquals(expectedResult, callHandleReadRequest())
    }

    @Test
    fun testDisconnectedWhileDiscoveringServices() {
        postHandleConnected()
        assertEquals(Success, callHandleConnectionRequest())

        val expectedResult = Error("connection lost")
        postHandleDisconnected(expectedResult)
        assertEquals(expectedResult, callHandleDiscoveryRequest())
    }

    @Test
    fun testDiscoverySuccessFollowedByDisconnection() {
        postHandleConnected()
        assertEquals(Success, callHandleConnectionRequest())

        runDelayed(delay = DEFAULT_DELAY) {
            reportTime("handleServicesDiscovered")
            responder.handleServicesDiscovered()
            reportTime("handleDisconnected")
            responder.handleDisconnected(Error("connection lost"))
        }
        assertEquals(Success, callHandleDiscoveryRequest())
    }

    @Test
    fun testWontStartNewOperationUntilPreviousOneIsRunning() {
        runDelayed(delay = SHORT_TIMEOUT) {
            reportTime("handleDisconnectionRequest")
            assertEquals(Busy, requester.handleDisconnectionRequest(SHORT_TIMEOUT))
            reportTime("handleDisconnectionRequest")
            responder.handleConnected()
        }
        assertEquals(Success, callHandleConnectionRequest())
    }
}
