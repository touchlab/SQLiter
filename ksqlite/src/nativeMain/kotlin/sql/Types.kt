package sql

import cnames.structs.sqlite3_stmt
import kotlinx.cinterop.CPointer

typealias SqliteDatabasePointer = CPointer<sqlite3.sqlite3>
typealias SqliteStatementPointer = CPointer<sqlite3_stmt>

fun ALOGE(message:String){
    println(message)
}

fun ALOGV(message:String){
    println(message)
}