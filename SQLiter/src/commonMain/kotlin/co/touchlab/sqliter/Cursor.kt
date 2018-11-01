package co.touchlab.sqliter

/**
 * Simplified Cursor implementation. Forward-only traversal.
 */
interface Cursor {
    fun next(): Boolean
    fun isNull(index: Int): Boolean
    fun getString(index: Int): String
    fun getLong(index: Int): Long
    fun getBytes(index: Int): ByteArray
    fun getDouble(index: Int): Double
    val columnCount: Int
    fun columnName(index: Int): String
}

fun Cursor.getStringOrNull(index: Int): String?{
    return if(isNull(index))
        null
    else
        getString(index)
}

fun Cursor.getLongOrNull(index: Int): Long?{
    return if(isNull(index))
        null
    else
        getLong(index)
}

fun Cursor.getBytesOrNull(index: Int): ByteArray?{
    return if(isNull(index))
        null
    else
        getBytes(index)
}

fun Cursor.getDoubleOrNull(index: Int): Double?{
    return if(isNull(index))
        null
    else
        getDouble(index)
}

val Cursor.columnNames: Array<String>
    get() = Array(columnCount) {
        columnName(it)
    }