package co.touchlab.sqliter.util

/**
 * Wrapper for platform.posix.usleep
 * Commonization issue can be tracked https://youtrack.jetbrains.com/issue/KT-48278
 */
internal expect fun usleep(seconds: UInt)
