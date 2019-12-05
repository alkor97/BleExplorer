package info.alkor.bleinquirer.bluetooth.handler.impl

class BinaryState {

    private var value = false

    fun writer() = fun(newValue: Boolean) {
        value = newValue
    }

    fun reader() = fun() = value
}
