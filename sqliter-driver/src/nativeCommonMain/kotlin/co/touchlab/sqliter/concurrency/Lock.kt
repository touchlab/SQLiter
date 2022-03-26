package co.touchlab.sqliter.concurrency

/**
 * A simple lock.
 * Implementations of this class should be re-entrant.
 */
internal expect class Lock() {
    fun lock()
    fun unlock()
    fun tryLock(): Boolean
}

internal expect inline fun Lock.close()

internal inline fun <T> Lock.withLock(block: () -> T): T {
    lock()
    try {
        return block()
    } finally {
        unlock()
    }
}