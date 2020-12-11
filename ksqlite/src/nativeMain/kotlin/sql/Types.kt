package sql

import cnames.structs.sqlite3_stmt
import kotlinx.cinterop.CPointer

typealias SqliteDatabase = CPointer<sqlite3.sqlite3>
typealias SqliteStatement = CPointer<sqlite3_stmt>

fun ALOGE(message:String){
    println(message)
}

fun ALOGV(message:String){
    println(message)
}