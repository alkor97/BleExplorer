package com.example.bleinquirer

import java.util.*

object GattServices {
    private fun makeStandardService(shortServiceId: Int) = "%08x-0000-1000-8000-00805f9b34fb".format(shortServiceId)

    val GENERIC_ACCESS = UUID.fromString(makeStandardService(0x1800))
    val GENERIC_ATTRIBUTE = UUID.fromString(makeStandardService(0x1801))
    val IMMEDIATE_ALERT = UUID.fromString(makeStandardService(0x1802))
    val LINK_LOSS = UUID.fromString(makeStandardService(0x1803))
    val TX_POWER = UUID.fromString(makeStandardService(0x1804))
    val CURRENT_TIME = UUID.fromString(makeStandardService(0x1805))
    val REFERENCE_TIME_UPDATE = UUID.fromString(makeStandardService(0x1806))
    val NEXT_DST_CHANGE = UUID.fromString(makeStandardService(0x1807))
    val GLUCOSE = UUID.fromString(makeStandardService(0x1808))
    val HEALTH_THERMOMETER = UUID.fromString(makeStandardService(0x1809))
    val DEVICE_INFORMATION = UUID.fromString(makeStandardService(0x180a))
    val HEART_RATE = UUID.fromString(makeStandardService(0x180d))
    val PHONE_ALERT_STATUS = UUID.fromString(makeStandardService(0x180e))
    val BATTERY_SERVICE = UUID.fromString(makeStandardService(0x180f))
    val BLOOD_PRESSURE = UUID.fromString(makeStandardService(0x1810))
    val ALERT_NOTIFICATION = UUID.fromString(makeStandardService(0x1811))
    val HUMAN_INTERFACE_DEVICE = UUID.fromString(makeStandardService(0x1812))
    val SCAN_PARAMETERS = UUID.fromString(makeStandardService(0x1813))
    val RUNNING_SPEED_AND_CADENCE = UUID.fromString(makeStandardService(0x1814))
    val AUTOMATION_IO = UUID.fromString(makeStandardService(0x1815))
    val CYCLIC_SPEED_AND_CADENCE = UUID.fromString(makeStandardService(0x1816))
    val CYCLIC_POWER = UUID.fromString(makeStandardService(0x1818))
    val LOCATION_AND_NAVIGATION = UUID.fromString(makeStandardService(0x1819))
    val ENVIRONMENTAL_SENSING = UUID.fromString(makeStandardService(0x181a))
    val BODY_COMPOSITION = UUID.fromString(makeStandardService(0x181b))
    val USER_DATA = UUID.fromString(makeStandardService(0x181c))
    val WEIGHT_SCALE = UUID.fromString(makeStandardService(0x181d))
    val BOND_MANAGEMENT = UUID.fromString(makeStandardService(0x181e))
    val CONTINUOUS_GLUCODE_MONITORING = UUID.fromString(makeStandardService(0x181f))
    val INTERNET_PROTOCOL_SUPPORT = UUID.fromString(makeStandardService(0x1820))
    val INDOOR_POSITIONING = UUID.fromString(makeStandardService(0x1821))
    val PULSE_OXIMETER = UUID.fromString(makeStandardService(0x1822))
    val HTTP_PROXY = UUID.fromString(makeStandardService(0x1823))
    val TRANSPORT_DISCOVERY = UUID.fromString(makeStandardService(0x1824))
    val OBJECT_TRANSFER = UUID.fromString(makeStandardService(0x1825))
    val FITNESS_MACHINE = UUID.fromString(makeStandardService(0x1826))
    val MESH_PROVISIONING = UUID.fromString(makeStandardService(0x1827))
    val MESH_PROXY = UUID.fromString(makeStandardService(0x1828))
    val RECONNECTION_CONFIGURATION = UUID.fromString(makeStandardService(0x1829))
    val INSULIN_DELIVERY = UUID.fromString(makeStandardService(0x183a))
    val BINARY_SENSOR = UUID.fromString(makeStandardService(0x183b))
    val EMERGENCY_CONNFIGURATION = UUID.fromString(makeStandardService(0x183c))

    val XIAOMI_ROOT_SERVICE = UUID.fromString("0000fe95-0000-1000-8000-00805f9b34fb")
    val XIAOMI_DATA_SERVICE = UUID.fromString("00001204-0000-1000-8000-00805f9b34fb")
    val XIAOMI_MIJIA_DATA_SERVICE = UUID.fromString("226c0000-6476-4566-7562-66734470666d")
    val NORDIC_DEVICE_FIRMWARE_UPDATE = UUID.fromString("00001530-1212-efde-1523-785feabcd123")

    val NAMES = hashMapOf(
        GENERIC_ACCESS.toString() to "Generic Access",
        GENERIC_ATTRIBUTE.toString() to "Generic Attribute",
        IMMEDIATE_ALERT.toString() to "Immediate Alert",
        LINK_LOSS.toString() to "Link Loss",
        TX_POWER.toString() to "Tx Power",
        CURRENT_TIME.toString() to "Current Time",
        REFERENCE_TIME_UPDATE.toString() to "Reference Time Update",
        NEXT_DST_CHANGE.toString() to "Next DST Change",
        GLUCOSE.toString() to "Glucose",
        HEALTH_THERMOMETER.toString() to "Health Thermometer",
        DEVICE_INFORMATION.toString() to "Device Information",
        HEART_RATE.toString() to "Heart Rate",
        PHONE_ALERT_STATUS.toString() to "Phone Alert Status",
        BATTERY_SERVICE.toString() to "Battery Service",
        BLOOD_PRESSURE.toString() to "Blood Pressure",
        ALERT_NOTIFICATION.toString() to "Alert Notification",
        HUMAN_INTERFACE_DEVICE.toString() to "Human Interface Device",
        SCAN_PARAMETERS.toString() to "Scan Parameters",
        RUNNING_SPEED_AND_CADENCE.toString() to "Running Speed and Cadence",
        AUTOMATION_IO.toString() to "Automation IO",
        CYCLIC_SPEED_AND_CADENCE.toString() to "Cycling Speed and Cadence",
        CYCLIC_POWER.toString() to "Cycling Power",
        LOCATION_AND_NAVIGATION.toString() to "Location and Navigation",
        ENVIRONMENTAL_SENSING.toString() to "Environmental Sensing",
        BODY_COMPOSITION.toString() to "Body Composition",
        USER_DATA.toString() to "User Data",
        WEIGHT_SCALE.toString() to "Weight Scale",
        BOND_MANAGEMENT.toString() to "Bond Management",
        CONTINUOUS_GLUCODE_MONITORING.toString() to "Continuous Glucose Monitoring",
        INTERNET_PROTOCOL_SUPPORT.toString() to "Internet Protocol Support",
        INDOOR_POSITIONING.toString() to "Indoor Positioning",
        PULSE_OXIMETER.toString() to "Pulse Oximeter",
        HTTP_PROXY.toString() to "HTTP Proxy",
        TRANSPORT_DISCOVERY.toString() to "Transport Discovery",
        OBJECT_TRANSFER.toString() to "Object Transfer",
        FITNESS_MACHINE.toString() to "Fitness Machine",
        MESH_PROVISIONING.toString() to "Mesh Provisioning",
        MESH_PROXY.toString() to "Mesh Proxy",
        RECONNECTION_CONFIGURATION.toString() to "Reconnection Configuration",
        INSULIN_DELIVERY.toString() to "Insulin Delivery",
        BINARY_SENSOR.toString() to "Binary Sensor",
        EMERGENCY_CONNFIGURATION.toString() to "Emergency Configuration",
        XIAOMI_ROOT_SERVICE.toString() to "Xiaomi Root Service",
        XIAOMI_DATA_SERVICE.toString() to "Xiaomi Data Service",
        NORDIC_DEVICE_FIRMWARE_UPDATE.toString() to "Nordic Device Firmware Update",
        XIAOMI_MIJIA_DATA_SERVICE.toString() to "Xiaomi Mijia Data Service"
    )

    fun getName(uuid: String): String {
        val name =  NAMES[uuid]
        if (name != null) {
            return name
        }
        return uuid
    }

    fun getName(uuid: UUID): String {
        return getName(uuid.toString())
    }
}