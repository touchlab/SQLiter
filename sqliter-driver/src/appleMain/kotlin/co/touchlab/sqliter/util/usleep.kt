package co.touchlab.sqliter.util

internal actual fun usleep(seconds: UInt) {
    platform.posix.usleep(seconds)
}