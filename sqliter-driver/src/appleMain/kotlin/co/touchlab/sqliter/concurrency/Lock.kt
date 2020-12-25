package co.touchlab.sqliter.concurrency

import platform.Foundation.NSRecursiveLock

actual typealias Lock = NSRecursiveLock

@Suppress("NOTHING_TO_INLINE")
actual inline fun Lock.close() {}