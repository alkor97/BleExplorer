package info.alkor.bleinquirer.bluetooth.handler.impl

import info.alkor.bleinquirer.bluetooth.handler.Busy
import info.alkor.bleinquirer.bluetooth.handler.Result
import java.util.concurrent.atomic.AtomicReference

class Operation {
    enum class Type {
        NONE, CONNECT, DISCOVER, READ, DISCONNECT
    }

    private val currentOperation = AtomicReference(Type.NONE)

    private fun start(operation: Type, block: () -> Result): Result {
        if (!currentOperation.compareAndSet(Type.NONE, operation))
            return Busy
        try {
            return block()
        } finally {
            currentOperation.set(Type.NONE)
        }
    }

    fun guard() = fun(operation: Type, block: () -> Result) = start(operation, block)
}
