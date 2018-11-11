package co.touchlab.sqliter

class NativeCursor(private val statement: NativeStatement) : Cursor {
    private var nextCalled = false
    override fun next(): Boolean {
        nextCalled = true
        return nativeStep(statement.connection.connectionPtr, statement.statementPtr)
    }
    override fun isNull(index: Int): Boolean = checkNextCalled{nativeIsNull(statement.statementPtr, index)}
    override fun getString(index: Int): String = checkNextCalled{nativeColumnGetString(statement.statementPtr, index)}
    override fun getLong(index: Int): Long = checkNextCalled{nativeColumnGetLong(statement.statementPtr, index)}
    override fun getBytes(index: Int): ByteArray = checkNextCalled{nativeColumnGetBlob(statement.statementPtr, index)}
    override fun getDouble(index: Int): Double = checkNextCalled{nativeColumnGetDouble(statement.statementPtr, index)}
    override fun getType(index: Int): FieldType = checkNextCalled{FieldType.forCode(nativeColumnType(statement.statementPtr, index))}
    override val columnCount: Int
        get() = nativeColumnCount(statement.statementPtr)

    override fun columnName(index: Int): String = nativeColumnName(statement.statementPtr, index)
    override fun close() {
        statement.reset()
    }

    override val columnNames: Map<String, Int> by lazy {
        val map = HashMap<String, Int>(this.columnCount)
        for (i in 0 until columnCount) {
            map.put(columnName(i), i)
        }
        map
    }

    private inline fun <R> checkNextCalled(block:()->R):R{
        if(!nextCalled){
            throw IllegalStateException("next() must be called before first result")
        }
        return block()
    }
}

@SymbolName("SQLiter_SQLiteConnection_nativeColumnIsNull")
private external fun nativeIsNull(statementPtr: Long, index: Int): Boolean

@SymbolName("SQLiter_SQLiteConnection_nativeColumnGetLong")
private external fun nativeColumnGetLong(statementPtr: Long, index: Int): Long

@SymbolName("SQLiter_SQLiteConnection_nativeColumnGetDouble")
private external fun nativeColumnGetDouble(statementPtr: Long, index: Int): Double

@SymbolName("SQLiter_SQLiteConnection_nativeColumnGetString")
private external fun nativeColumnGetString(statementPtr: Long, index: Int): String

@SymbolName("SQLiter_SQLiteConnection_nativeColumnGetBlob")
private external fun nativeColumnGetBlob(statementPtr: Long, index: Int): ByteArray

@SymbolName("SQLiter_SQLiteConnection_nativeColumnCount")
private external fun nativeColumnCount(statementPtr: Long): Int

@SymbolName("SQLiter_SQLiteConnection_nativeColumnName")
private external fun nativeColumnName(statementPtr: Long, index: Int): String

@SymbolName("SQLiter_SQLiteConnection_nativeColumnType")
private external fun nativeColumnType(statementPtr: Long, index: Int): Int

@SymbolName("SQLiter_SQLiteConnection_nativeStep")
internal external fun nativeStep(connectionPtr: Long, statementPtr: Long): Boolean