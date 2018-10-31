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

val Cursor.columnNames: Array<String>
    get() = Array(columnCount) {
        columnName(it)
    }