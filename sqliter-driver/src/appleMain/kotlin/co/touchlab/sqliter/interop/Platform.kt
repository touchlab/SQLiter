package co.touchlab.sqliter.interop

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.autoreleasepool
import platform.Foundation.NSString
import platform.Foundation.create

actual inline fun bytesToString(bv: CPointer<ByteVar>): String = autoreleasepool {
    NSString.create(uTF8String = bv).toString()
}