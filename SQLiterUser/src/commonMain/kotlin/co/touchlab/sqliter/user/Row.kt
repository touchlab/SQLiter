package co.touchlab.sqliter.user

import co.touchlab.sqliter.FieldType

interface Row{
    fun isNull(index: Int): Boolean
    fun string(index: Int): String
    fun long(index: Int): Long
    fun bytes(index: Int): ByteArray
    fun double(index: Int): Double
    fun type(index: Int): FieldType
    val columnCount: Int
    fun columnName(index: Int): String
    val columnNames: Map<String, Int>
}