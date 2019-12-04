package com.example.bleinquirer

import java.util.concurrent.TimeUnit

data class Timeout(
    val value: Long,
    val unit: TimeUnit = TimeUnit.SECONDS
) {
    val humanReadable: String
        get() = "%d%s".format(value, unit.humanReadable)

    fun to(targetUnit: TimeUnit) = Timeout(targetUnit.convert(value, unit))
}

val TimeUnit.humanReadable: String
    get() = when (this) {
        TimeUnit.NANOSECONDS -> "ns"
        TimeUnit.MICROSECONDS -> "us"
        TimeUnit.MILLISECONDS -> "ms"
        TimeUnit.SECONDS -> "s"
        TimeUnit.MINUTES -> "min"
        TimeUnit.HOURS -> "h"
        TimeUnit.DAYS -> "d"
    }
