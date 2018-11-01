package co.touchlab.sqliter

import kotlinx.coroutines.delay
import platform.posix.usleep
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.native.concurrent.FutureState
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.freeze

class NativeCursor(private val statement: NativeStatement):Cursor {


    /*override suspend fun next(): Boolean = suspendCoroutine {
        usleep(1000)
        it.resume(nativeStep(statement.connection.connectionPtr, statement.statementPtr))
    }*/

    val pair = Pair(statement.connection.connectionPtr, statement.statementPtr).freeze()
    override fun next(): Boolean = nativeStep(statement.connection.connectionPtr, statement.statementPtr)

    override suspend fun nextSuspend(): Boolean = nativeStep(statement.connection.connectionPtr, statement.statementPtr)
    /*override suspend fun nextSuspend(): Boolean {
        val conn = statement.connection.connectionPtr
        val stmt = statement.statementPtr
        val future = statement.connection.suspendWorker.execute(TransferMode.UNSAFE,

            {*//*Pair(conn, stmt).freeze()*//*pair}) {
            nativeStep(it.first, it.second)
//            nativeStepUgh(null, conn, stmt)
        }

        var retryCount = 0

        while (future.state != FutureState.COMPUTED && future.state != FutureState.CANCELLED){
            if(future.state == FutureState.INVALID)
                throw IllegalStateException("Future invalid?")

            retryCount++

            if(retryCount > 20)
                delay(20)
            else if(retryCount > 5)
                delay(5)
        }

        return future.state == FutureState.COMPUTED && future.result
    }*/

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
}
@SymbolName("SQLiter_SQLiteConnection_nativeStep")
internal external fun nativeStep(connectionPtr:Long, statementPtr:Long):Boolean