package co.touchlab.sqliter

import platform.posix.usleep
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class NativeCursor(private val statement: NativeStatement):Cursor {


    /*override suspend fun next(): Boolean = suspendCoroutine {
        usleep(1000)
        it.resume(nativeStep(statement.connection.connectionPtr, statement.statementPtr))
    }*/

    override fun next(): Boolean = nativeStep(statement.connection.connectionPtr, statement.statementPtr)

//    override suspend fun nextNonSuspend(): Boolean = nativeStep(statement.connection.connectionPtr, statement.statementPtr)

    /*override suspend fun next(): Boolean =
        it.resume(nativeStep(statement.connection.connectionPtr, statement.statementPtr))*/

//    override fun nextBlocking(): Boolean = nativeStep(statement.connection.connectionPtr, statement.statementPtr)

    override fun isNull(index: Int): Boolean = nativeIsNull(statement.statementPtr, index)
    override fun getString(index: Int): String = nativeColumnGetString(statement.statementPtr, index)
    override fun getLong(index: Int): Long = nativeColumnGetLong(statement.statementPtr, index)
    override fun getBytes(index: Int): ByteArray = nativeColumnGetBlob(statement.statementPtr, index)
    override fun getDouble(index: Int): Double = nativeColumnGetDouble(statement.statementPtr, index)
    override val columnCount: Int
        get() = nativeColumnCount(statement.statementPtr)

    override fun columnName(index: Int): String = nativeColumnName(statement.statementPtr, index)

    @SymbolName("SQLiter_SQLiteConnection_nativeColumnIsNull")
    private external fun nativeIsNull(statementPtr:Long, index:Int):Boolean

    @SymbolName("SQLiter_SQLiteConnection_nativeColumnGetLong")
    private external fun nativeColumnGetLong(statementPtr:Long, index:Int):Long

    @SymbolName("SQLiter_SQLiteConnection_nativeColumnGetDouble")
    private external fun nativeColumnGetDouble(statementPtr:Long, index:Int):Double

    @SymbolName("SQLiter_SQLiteConnection_nativeColumnGetString")
    private external fun nativeColumnGetString(statementPtr:Long, index:Int):String

    @SymbolName("SQLiter_SQLiteConnection_nativeColumnGetBlob")
    private external fun nativeColumnGetBlob(statementPtr:Long, index:Int):ByteArray

    @SymbolName("SQLiter_SQLiteConnection_nativeColumnCount")
    private external fun nativeColumnCount(statementPtr:Long):Int

    @SymbolName("SQLiter_SQLiteConnection_nativeColumnName")
    private external fun nativeColumnName(statementPtr:Long, index: Int):String

    @SymbolName("SQLiter_SQLiteConnection_nativeStep")
    private external fun nativeStep(connectionPtr:Long, statementPtr:Long):Boolean
}
