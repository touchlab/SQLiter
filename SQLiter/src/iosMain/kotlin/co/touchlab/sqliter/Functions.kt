package co.touchlab.sqliter

import kotlin.native.internal.ExportForCppRuntime

@ExportForCppRuntime
fun ThrowSql_SQLiteException(exceptionClass:String, message:String): Unit =
    throw Exception("$exceptionClass - $message")