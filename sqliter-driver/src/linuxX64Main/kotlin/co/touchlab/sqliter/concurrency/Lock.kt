package co.touchlab.sqliter.concurrency

import co.touchlab.sqliter.util.maybeFreeze
import kotlinx.cinterop.Arena
import kotlinx.cinterop.alloc
import kotlinx.cinterop.ptr
import platform.posix.*

/**
 * A simple lock.
 * Implementations of this class should be re-entrant.
 */
internal actual class Lock actual constructor() {
    private val arena = Arena()
    private val attr = arena.alloc<pthread_mutexattr_t>()
    private val mutex = arena.alloc<pthread_mutex_t>()

    init {
        pthread_mutexattr_init(attr.ptr)
        pthread_mutexattr_settype(attr.ptr, PTHREAD_MUTEX_RECURSIVE.toInt())
        pthread_mutex_init(mutex.ptr, attr.ptr)
        maybeFreeze()
    }

    actual fun lock() {
        pthread_mutex_lock(mutex.ptr)
    }

    actual fun unlock() {
        pthread_mutex_unlock(mutex.ptr)
    }

    actual fun tryLock(): Boolean = pthread_mutex_trylock(mutex.ptr) == 0

    fun internalClose() {
        pthread_mutex_destroy(mutex.ptr)
        pthread_mutexattr_destroy(attr.ptr)
        arena.clear()
    }
}

internal actual inline fun Lock.close() {
    internalClose()
}