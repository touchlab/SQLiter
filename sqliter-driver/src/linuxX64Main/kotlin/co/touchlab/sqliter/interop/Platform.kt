package co.touchlab.sqliter.interop

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.toKString

actual inline fun bytesToString(bv: CPointer<ByteVar>): String = bv.toKString()