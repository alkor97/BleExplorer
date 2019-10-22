package com.example.bleinquirer

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class ConnectionHandlerTemplateTest {

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

    private fun runDelayed(name: String = "", delay: Timeout = DEFAULT_DELAY, procedure: () -> Unit = {}) = GlobalScope.launch {
        delay(delay.unit.toMillis(delay.value))
        if (!name.isBlank()) {
            reportTime(name)
        }
        procedure()
    }

    private val instance = ConnectionHandlerTemplate()

    private fun postHandleConnected(delay: Timeout = DEFAULT_DELAY) = runDelayed("handleConnected", delay) {instance.handleConnected()}
    private fun callHandleConnectionRequest(timeout: Timeout = DEFAULT_TIMEOUT): Result {
        reportTime("handleConnectionRequest")
        return instance.handleConnectionRequest(timeout)
    }

    private fun postHandleServicesDiscovered(delay: Timeout = DEFAULT_DELAY) = runDelayed("handleServicesDiscovered", delay) {instance.handleServicesDiscovered()}
    private fun postHandleServicesDiscoveryError(result: Result, delay: Timeout = DEFAULT_DELAY) = runDelayed("handleServicesDiscoveryError", delay) {instance.handleServicesDiscoveryError(result)}
    private fun callHandleDiscoveryRequest(timeout: Timeout = DEFAULT_TIMEOUT): Result {
        reportTime("handleDiscoveryRequest")
        return instance.handleDiscoveryRequest(timeout)
    }

    private fun postHandleReadingSuccess(delay: Timeout = DEFAULT_DELAY) = runDelayed("handleReadingSuccess", delay) {instance.handleReadingSuccess()}
    private fun callHandleReadRequest(timeout: Timeout = DEFAULT_TIMEOUT): Boolean {
        reportTime("handleReadRequest")
        return instance.handleReadRequest(timeout) == Success
    }

    private fun postHandleDisconnected(result: Result = Success, delay: Timeout = DEFAULT_DELAY) = runDelayed("handleDisconnected", delay) {instance.handleDisconnected(result)}
    private fun callHandleDisconnectionRequest(timeout: Timeout = DEFAULT_TIMEOUT): Boolean {
        reportTime("handleDisconnectionRequest")
        return instance.handleDisconnectionRequest(timeout) == Success
    }

    @Test
    fun testHappyPath() {
        postHandleConnected()
        assertTrue(callHandleConnectionRequest() is Success)

        postHandleServicesDiscovered()
        assertTrue(callHandleDiscoveryRequest() is Success)

        postHandleReadingSuccess()
        assertTrue(callHandleReadRequest())

        postHandleDisconnected()
        assertTrue(callHandleDisconnectionRequest())
    }

    @Test
    fun testConnectionTimeout() {
        assertFalse(callHandleConnectionRequest(SHORT_TIMEOUT) is Success)
    }

    @Test
    fun testServiceDiscoveryTimeout() {
        postHandleConnected()
        assertTrue(callHandleConnectionRequest() is Success)

        assertTrue(callHandleDiscoveryRequest(SHORT_TIMEOUT) is TimedOut)
    }

    @Test
    fun testServiceDiscoveryError() {
        postHandleConnected()
        assertTrue(callHandleConnectionRequest() is Success)

        val expectedResult = Error("discovery error")
        postHandleServicesDiscoveryError(expectedResult)
        assertEquals(expectedResult, callHandleDiscoveryRequest())
    }

    @Test
    fun testReadingTimeout() {
        postHandleConnected()
        assertTrue(callHandleConnectionRequest() is Success)

        postHandleServicesDiscovered()
        assertTrue(callHandleDiscoveryRequest() is Success)

        postHandleReadingSuccess()
        assertTrue(callHandleReadRequest())

        assertFalse(callHandleDisconnectionRequest(SHORT_TIMEOUT))
    }

    @Test
    fun testDisconnectedWhileDiscoveringServices() {
        postHandleConnected()
        assertTrue(callHandleConnectionRequest() is Success)

        val expectedResult = Error("connection lost")
        postHandleDisconnected(expectedResult)
        assertEquals(expectedResult, callHandleDiscoveryRequest())
    }

    @Test
    fun testDiscoverySuccessFollowedByDisconnection() {
        postHandleConnected()
        assertTrue(callHandleConnectionRequest() is Success)

        runDelayed(delay=DEFAULT_DELAY) {
            reportTime("handleServicesDiscovered")
            instance.handleServicesDiscovered()
            reportTime("handleDisconnected")
            instance.handleDisconnected(Error("connection lost"))
        }
        assertEquals(Success, callHandleDiscoveryRequest())
    }

    @Test
    fun testWontStartNewOperationUntilPreviousOneIsRunning() {
        runDelayed(delay=SHORT_TIMEOUT) {
            reportTime("handleDisconnectionRequest")
            assertEquals(Busy, instance.handleDisconnectionRequest(SHORT_TIMEOUT))
            reportTime("handleDisconnectionRequest")
            instance.handleConnected()
        }
        assertEquals(Success, callHandleConnectionRequest())
    }
}
