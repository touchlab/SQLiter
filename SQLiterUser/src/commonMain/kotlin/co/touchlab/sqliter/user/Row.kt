package co.touchlab.sqliter.user

import co.touchlab.sqliter.FieldType

interface Row{
    fun isNull(index: Int): Boolean
    fun getString(index: Int): String
    fun getLong(index: Int): Long
    fun getBytes(index: Int): ByteArray
    fun getDouble(index: Int): Double
    fun getType(index: Int): FieldType
    val columnCount: Int
    fun columnName(index: Int): String
    val columnNames: Map<String, Int>
}