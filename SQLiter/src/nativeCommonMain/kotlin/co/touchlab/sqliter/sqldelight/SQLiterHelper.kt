package co.touchlab.sqliter.sqldelight

import co.touchlab.sqliter.*
import co.touchlab.stately.collections.frozenHashMap
import co.touchlab.stately.collections.frozenLinkedList
import co.touchlab.stately.concurrency.ThreadLocalRef
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.*
import kotlin.native.concurrent.freeze

class SQLiterHelper(private val databaseManager: DatabaseManager) : SqlDatabase {
    private val transactions = ThreadLocalRef<SQLiterConnection.Transaction>()

    override fun close() {
        databaseManager.close()
    }

    override fun getConnection(): SqlDatabaseConnection =
        SQLiterConnection(databaseManager.createConnection(), transactions)
}

class SQLiterConnection(
    private val databaseConnection: DatabaseConnection,
    private val transactions: ThreadLocalRef<Transaction> = ThreadLocalRef()
) : SqlDatabaseConnection {

    override fun currentTransaction(): Transacter.Transaction? = transactions.value

    override fun newTransaction(): Transacter.Transaction {
        databaseConnection.beginTransaction()
        val transaction = Transaction(transactions.value)
        transactions.value = transaction
        return transaction
    }

    override fun prepareStatement(sql: String, type: SqlPreparedStatement.Type, parameters: Int): SqlPreparedStatement {
        return SQLiterStatement(databaseConnection.createStatement(sql), type)
    }

    inner class Transaction(
        override val enclosingTransaction: SQLiterConnection.Transaction?
    ) : Transacter.Transaction() {
        override fun endTransaction(successful: Boolean) {
            if (enclosingTransaction == null) {
                if (successful) {
                    databaseConnection.setTransactionSuccessful()
                    databaseConnection.endTransaction()
                } else {
                    databaseConnection.endTransaction()
                }
            }
            transactions.value = enclosingTransaction
        }
    }
}

class SQLiterStatement(private val statement: Statement, private val type: SqlPreparedStatement.Type) :
    SqlPreparedStatement {
    private val binds = frozenHashMap<Int, (Statement) -> Unit>()

    override fun bindBytes(index: Int, bytes: ByteArray?) {
        bytes.freeze()
        binds.put(index) { it.bindBlob(index, bytes) }
    }

    override fun bindDouble(index: Int, double: Double?) {
        binds.put(index) { it.bindDouble(index, double) }
    }

    override fun bindLong(index: Int, long: Long?) {
        binds.put(index) { it.bindLong(index, long) }
    }

    override fun bindString(index: Int, string: String?) {
        binds.put(index) { it.bindString(index, string) }
    }

    private fun bindTo() {
        for (bind in binds.values.iterator()) {
            bind(statement)
        }
    }

    override fun execute(): Long {
        bindTo()
        return when (type) {

            SqlPreparedStatement.Type.INSERT -> {
                statement.executeInsert()
            }
            SqlPreparedStatement.Type.UPDATE, SqlPreparedStatement.Type.DELETE -> {
                statement.executeUpdateDelete().toLong()
            }
            SqlPreparedStatement.Type.EXEC -> {
                statement.execute()
                1
            }
            SqlPreparedStatement.Type.SELECT -> throw AssertionError()
        }
    }

    override fun executeQuery(): SqlResultSet {
        bindTo()
        return SQLiterCursor(statement)
    }
}

class SQLiterCursor(private val statement: Statement) : SqlResultSet {
    private val cursor = statement.query()

    override fun close() {
        cursor.close()
    }

    override fun getBytes(index: Int): ByteArray? = cursor.getBytesOrNull(index)

    override fun getDouble(index: Int): Double? = cursor.getDoubleOrNull(index)

    override fun getLong(index: Int): Long? = cursor.getLongOrNull(index)

    override fun getString(index: Int): String? = cursor.getStringOrNull(index)

    override fun next(): Boolean = cursor.next()
}