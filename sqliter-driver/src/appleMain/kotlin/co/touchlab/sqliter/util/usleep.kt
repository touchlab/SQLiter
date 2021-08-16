package co.touchlab.sqliter.util

actual fun usleep(seconds: UInt) {
    platform.posix.usleep(seconds)
}