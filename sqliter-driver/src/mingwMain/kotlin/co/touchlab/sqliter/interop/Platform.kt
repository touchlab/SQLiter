package co.touchlab.sqliter.interop

import kotlinx.cinterop.*

actual inline fun bytesToString(bv:CPointer<ByteVar>):String = bv.toKStringFromUtf8()