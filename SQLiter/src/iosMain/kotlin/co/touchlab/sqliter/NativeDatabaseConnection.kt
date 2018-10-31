package co.touchlab.sqliter

import co.touchlab.stately.collections.AbstractSharedLinkedList
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze


class NativeDatabaseConnection(private val dbManager:NativeDatabaseManager, internal val connectionPtr:Long):DatabaseConnection{


    internal val meNode:AtomicReference<AbstractSharedLinkedList.Node<NativeDatabaseConnection>?> = AtomicReference(null)
    internal val transaction = AtomicReference<Transaction?>(null)

    data class Transaction(val successful:Boolean)

    override fun createStatement(sql: String): Statement {
        val statementPtr = nativePrepareStatement(connectionPtr, sql)
        val statement = NativeStatement(this, statementPtr)

        return statement
    }

    override fun beginTransaction() {
        transaction.value = Transaction(false).freeze()
        withStatement("BEGIN;"){it.execute()}
    }

    override fun setTransactionSuccessful() {
        val trans = checkFailTransaction()

        transaction.value = trans.copy(successful = true).freeze()
    }

    override fun endTransaction() {
        val trans = checkFailTransaction()

        try {
            withStatement(if(trans.successful){"COMMIT;"}else{"ROLLBACK;"}){it.execute()}
        } finally {
            transaction.value = null
        }
    }

    private fun checkFailTransaction(): Transaction {
        val trans = transaction.value
        if (trans == null)
            throw Exception("No transaction")
        return trans!!
    }

    override fun close() {
        val node = meNode.value
        if(node != null && !node.isRemoved)
            node.remove()
        nativeClose(connectionPtr)
    }

    @SymbolName("Android_Database_SQLiteConnection_nativePrepareStatement")
    private external fun nativePrepareStatement(connectionPtr:Long, sql:String):Long

    @SymbolName("Android_Database_SQLiteConnection_nativeClose")
    private external fun nativeClose(connectionPtr:Long)
}