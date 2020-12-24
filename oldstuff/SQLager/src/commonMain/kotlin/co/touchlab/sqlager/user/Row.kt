package co.touchlab.sqlager.user

import co.touchlab.sqliter.FieldType

interface Row {
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

fun Row.nameIndex(name: String): Int {
    val index = columnNames.get(name)
    if (index == null)
        throw IllegalArgumentException("Name $name not found in results")
    else
        return index
}

fun Row.int(index: Int): Int = long(index).toInt()
fun Row.float(index: Int): Float = double(index).toFloat()

fun Row.stringOrNull(index: Int): String? = if(isNull(index)){null}else{string(index)}
fun Row.longOrNull(index: Int): Long? = if(isNull(index)){null}else{long(index)}
fun Row.bytesOrNull(index: Int): ByteArray? = if(isNull(index)){null}else{bytes(index)}
fun Row.doubleOrNull(index: Int): Double? = if(isNull(index)){null}else{double(index)}
fun Row.intOrNull(index: Int): Int? = longOrNull(index)?.toInt()
fun Row.floatOrNull(index: Int): Float? = doubleOrNull(index)?.toFloat()

fun Row.isNull(name: String): Boolean = isNull(nameIndex(name))

fun Row.string(name: String): String = string(nameIndex(name))
fun Row.long(name: String): Long = long(nameIndex(name))
fun Row.bytes(name: String): ByteArray = bytes(nameIndex(name))
fun Row.double(name: String): Double = double(nameIndex(name))
fun Row.int(name: String): Int = long(name).toInt()
fun Row.float(name: String): Float = double(name).toFloat()

fun Row.stringOrNull(name: String): String? = stringOrNull(nameIndex(name))
fun Row.longOrNull(name: String): Long? = longOrNull(nameIndex(name))
fun Row.bytesOrNull(name: String): ByteArray? = bytesOrNull(nameIndex(name))
fun Row.doubleOrNull(name: String): Double? = doubleOrNull(nameIndex(name))
fun Row.intOrNull(name: String): Int? = longOrNull(name)?.toInt()
fun Row.floatOrNull(name: String): Float? = doubleOrNull(name)?.toFloat()