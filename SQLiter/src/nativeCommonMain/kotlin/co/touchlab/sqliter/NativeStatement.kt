package co.touchlab.sqliter

class NativeStatement(
    internal val connection: NativeDatabaseConnection,
    nativePointer:Long):NativePointer(nativePointer), Statement {
    

    override fun execute() {
        try {
            nativeExecute(connection.nativePointer, nativePointer)
        } finally {
            resetStatement()
            clearBindings()
        }
    }

    override fun executeInsert():Long = try {
        nativeExecuteForLastInsertedRowId(connection.nativePointer, nativePointer)
    } finally {
        resetStatement()
        clearBindings()
    }

    override fun executeUpdateDelete():Int = try {
        nativeExecuteForChangedRowCount(connection.nativePointer, nativePointer)
    } finally {
        resetStatement()
        clearBindings()
    }

    override fun query(): Cursor = NativeCursor(this)

    override fun finalizeStatement() {
        closeNativePointer()
    }

    override fun actualClose(nativePointerArg: Long) {
        nativeFinalizeStatement(connection.nativePointer, nativePointerArg)
    }

    override fun resetStatement() {
        nativeResetStatement(connection.nativePointer, nativePointer)
    }

    override fun clearBindings() {
        nativeClearBindings(connection.nativePointer, nativePointer)
    }

    override fun bindNull(index: Int) {
        nativeBindNull(connection.nativePointer, nativePointer, index)
    }

    override fun bindLong(index: Int, value: Long) {
        nativeBindLong(connection.nativePointer, nativePointer, index, value)
    }

    override fun bindDouble(index: Int, value: Double) {
        nativeBindDouble(connection.nativePointer, nativePointer, index, value)
    }

    override fun bindString(index: Int, value: String) {
        nativeBindString(connection.nativePointer, nativePointer, index, value)
    }

    override fun bindBlob(index: Int, value: ByteArray) {
        nativeBindBlob(connection.nativePointer, nativePointer, index, value)
    }

    override fun bindParameterIndex(paramName: String): Int {
        val index = nativeBindParameterIndex(nativePointer, paramName)
        if(index == 0)
            throw IllegalArgumentException("Statement parameter $paramName not found")
        return index
    }
}

@SymbolName("SQLiter_SQLiteStatement_nativeFinalizeStatement")
private external fun nativeFinalizeStatement(connectionPtr:Long, nativePointer:Long)

@SymbolName("SQLiter_SQLiteConnection_nativeBindParameterIndex")
private external fun nativeBindParameterIndex(nativePointer:Long, paramName:String):Int

@SymbolName("SQLiter_SQLiteConnection_nativeResetStatement")
private external fun nativeResetStatement(connectionPtr:Long, nativePointer:Long)

@SymbolName("SQLiter_SQLiteConnection_nativeClearBindings")
private external fun nativeClearBindings(connectionPtr:Long, nativePointer:Long)

@SymbolName("SQLiter_SQLiteStatement_nativeExecute")
private external fun nativeExecute(connectionPtr:Long, nativePointer:Long)

@SymbolName("SQLiter_SQLiteStatement_nativeExecuteForChangedRowCount")
private external fun nativeExecuteForChangedRowCount(connectionPtr:Long, nativePointer:Long):Int

@SymbolName("SQLiter_SQLiteStatement_nativeExecuteForLastInsertedRowId")
private external fun nativeExecuteForLastInsertedRowId(
    connectionPtr:Long, nativePointer:Long):Long

@SymbolName("SQLiter_SQLiteStatement_nativeBindNull")
private external fun nativeBindNull(connectionPtr:Long, nativePointer:Long,
                                    index:Int)

@SymbolName("SQLiter_SQLiteStatement_nativeBindLong")
private external fun nativeBindLong(connectionPtr:Long, nativePointer:Long,
                                    index:Int, value:Long)
@SymbolName("SQLiter_SQLiteStatement_nativeBindDouble")
private external fun nativeBindDouble(connectionPtr:Long, nativePointer:Long,
                                      index:Int, value:Double)
@SymbolName("SQLiter_SQLiteStatement_nativeBindString")
private external fun nativeBindString(connectionPtr:Long, nativePointer:Long,
                                      index:Int, value:String)
@SymbolName("SQLiter_SQLiteStatement_nativeBindBlob")
private external fun nativeBindBlob(connectionPtr:Long, nativePointer:Long,
                                    index:Int, value:ByteArray)
