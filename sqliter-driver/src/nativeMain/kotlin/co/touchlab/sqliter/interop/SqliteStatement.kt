package co.touchlab.sqliter.interop

internal interface SqliteStatement {
    //Cursor methods
    fun isNull(index: Int): Boolean
    fun columnGetLong(columnIndex: Int): Long
    fun columnGetDouble(columnIndex: Int): Double
    fun columnGetString(columnIndex: Int): String
    fun columnGetBlob(columnIndex: Int): ByteArray
    fun columnCount(): Int
    fun columnName(columnIndex: Int): String
    fun columnType(columnIndex: Int): Int
    fun step(): Boolean

    //Statement methods
    fun finalizeStatement()
    fun bindParameterIndex(paramName: String): Int
    fun resetStatement()
    fun clearBindings()
    fun execute()
    fun executeForChangedRowCount(): Int
    fun executeForLastInsertedRowId(): Long
    fun bindNull(index: Int)
    fun bindLong(index: Int, value: Long)
    fun bindDouble(index: Int, value: Double)
    fun bindString(index: Int, value: String)
    fun bindBlob(index: Int, value: ByteArray)
    fun executeNonQuery(): Int

    fun traceLogCallback(message:String)
}