package co.touchlab.sqliter.internal

import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.windows.GetEnvironmentVariableW
import platform.windows.WCHARVar

internal class Utils {
    companion object {
        // Returns the path pointed to by the USERPROFILE environment variable.
        // We use this as a default when a path isn't provided.
        fun getUserDirectory(): String {
            return getEnvironmentVariable("USERPROFILE")
        }

        fun getEnvironmentVariable(variable: String): String {
            return memScoped {
                val size = 1024 * WCHARVar.size
                val pointer = allocArray<WCHARVar>(size)
                GetEnvironmentVariableW(variable, pointer, size.toUInt())
                pointer.toKString()
            }
        }
    }
}