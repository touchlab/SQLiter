package co.touchlab.sqliter.util

/**
 * Wrapper for platform.posix.usleep
 */
actual fun usleep(seconds: UInt) {
    platform.posix.usleep(seconds)
}