package co.touchlab.sqliter.interop

import kotlinx.cinterop.*
import platform.Foundation.NSString
import platform.Foundation.create

actual inline fun bytesToString(bv:CPointer<ByteVar>):String = NSString.create(uTF8String = bv).toString()