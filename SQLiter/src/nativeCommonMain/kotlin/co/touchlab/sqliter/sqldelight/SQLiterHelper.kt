package co.touchlab.sqliter.sqldelight

import co.touchlab.sqliter.*
import co.touchlab.stately.concurrency.ThreadLocalRef
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.*

class SQLiterHelper(private val databaseManager: DatabaseManager) : SqlDatabase {
    val CREATE_IF_NECESSARY = 0x10000000
    private val transactions = ThreadLocalRef<SQLiterConnection.Transaction>()

    override fun close() {
        databaseManager.close()
    }

    override fun getConnection(): SqlDatabaseConnection =
        SQLiterConnection(databaseManager.createConnection(CREATE_IF_NECESSARY), transactions)
}

class SQLiterConnection(private val databaseConnection: DatabaseConnection,
                        private val transactions: ThreadLocalRef<Transaction> = ThreadLocalRef()) : SqlDatabaseConnection {

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

    override fun bindBytes(index: Int, bytes: ByteArray?) {
        statement.bindBlob(index, bytes)
    }

    override fun bindDouble(index: Int, double: Double?) {
        statement.bindDouble(index, double)
    }

    override fun bindLong(index: Int, long: Long?) {
        statement.bindLong(index, long)
    }

    override fun bindString(index: Int, string: String?) {
        statement.bindString(index, string)
    }

    override fun execute() =
        when (type) {

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

    override fun executeQuery(): SqlResultSet = SQLiterCursor(statement)
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