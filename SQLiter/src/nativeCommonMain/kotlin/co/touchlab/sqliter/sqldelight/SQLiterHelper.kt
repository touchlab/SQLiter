package co.touchlab.sqliter.sqldelight

import co.touchlab.sqliter.*
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlDatabaseConnection
import com.squareup.sqldelight.db.SqlPreparedStatement
import kotlin.native.concurrent.AtomicReference

class SQLiterHelper(private val databaseManager: DatabaseManager): SqlDatabase {
    val CREATE_IF_NECESSARY = 0x10000000

    override fun close() {
        databaseManager.close()
    }

    override fun getConnection(): SqlDatabaseConnection =
        SQLiterConnection(databaseManager.createConnection(CREATE_IF_NECESSARY))
}

class SQLiterConnection(private val databaseConnection: DatabaseConnection): SqlDatabaseConnection{
    private val trans:AtomicReference<Transacter.Transaction?> = AtomicReference(null)

    override fun currentTransaction(): Transacter.Transaction? = trans.value

    override fun newTransaction(): Transacter.Transaction {
        val transaction = Transaction(trans.value)
        trans.value = transaction
        return transaction
    }

    override fun prepareStatement(sql: String, type: SqlPreparedStatement.Type, parameters: Int): SqlPreparedStatement =
        SQLiterStatement(databaseConnection.createStatement(sql))

    inner class Transaction(
        override val enclosingTransaction: Transacter.Transaction?
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
            trans.value = enclosingTransaction
        }
    }
}

class SQLiterStatement(private val statement: Statement):SqlPreparedStatement{

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

    override fun execute() {
        statement.execute()
    }

    override fun executeQuery(): SqlCursor = SQLiterCursor(statement)

}

class SQLiterCursor(private val statement: Statement):SqlCursor{
    private val cursor = statement.query()

    override fun close() {
        statement.finalize()
    }

    override fun getBytes(index: Int): ByteArray? = cursor.getBytesOrNull(index)

    override fun getDouble(index: Int): Double? = cursor.getDoubleOrNull(index)

    override fun getLong(index: Int): Long? = cursor.getLongOrNull(index)

    override fun getString(index: Int): String? = cursor.getStringOrNull(index)

    override fun next(): Boolean = cursor.next()
}