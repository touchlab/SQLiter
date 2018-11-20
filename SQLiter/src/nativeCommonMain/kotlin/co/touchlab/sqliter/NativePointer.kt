package co.touchlab.sqliter

import co.touchlab.stately.concurrency.QuickLock
import co.touchlab.stately.concurrency.withLock
import kotlin.native.concurrent.AtomicLong

abstract class NativePointer(nativePointerArg:Long){
    //Hold connection pointer in atomic and guard access to connection
    private val nativePointerActual = AtomicLong(nativePointerArg)

    private val pointerLock = QuickLock()
    internal val nativePointer: Long
        get() = pointerLock.withLock{
            val now = nativePointerActual.value
            if (now == 0L)
                throw IllegalStateException("Connection closed")
            return now
        }

    fun closeNativePointer() = pointerLock.withLock{
        val local = nativePointerActual.value
        nativePointerActual.value = 0
        actualClose(local)
    }

    abstract fun actualClose(nativePointerArg: Long)
}
