package co.touchlab.sqliter.util

/**
 * Wrapper for platform.posix.usleep
 */
internal actual fun usleep(seconds: UInt) {
    platform.posix.usleep(seconds)
}