package co.touchlab.sqliter.concurrency

import platform.Foundation.NSRecursiveLock

internal actual class Lock actual constructor() {
    private val l = NSRecursiveLock()
    actual fun lock() {
        l.lock()
    }

    actual fun unlock() {
        l.unlock()
    }

    actual fun tryLock(): Boolean = l.tryLock()
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun Lock.close() {}
