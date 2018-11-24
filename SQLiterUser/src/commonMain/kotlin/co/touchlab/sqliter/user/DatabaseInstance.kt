package co.touchlab.sqliter.user

import co.touchlab.sqliter.DatabaseConnection
import co.touchlab.sqliter.longForQuery
import co.touchlab.sqliter.stringForQuery
import co.touchlab.sqliter.withTransaction
import co.touchlab.stately.collections.frozenLruCache
import co.touchlab.stately.concurrency.AtomicBoolean
import co.touchlab.stately.concurrency.QuickLock
import co.touchlab.stately.concurrency.withLock

class DatabaseInstance internal constructor(
    private val connection: DatabaseConnection,
    private val cacheSize: Int = 200
) {

    private val closed = AtomicBoolean(false)
    private val statementCache = frozenLruCache<String, BinderStatement>(cacheSize) {
        it.value.statement.finalizeStatement()
    }

    private val cacheLock = QuickLock()

    fun execute(sql: String, bind: Binder.() -> Unit = {}) {
        safeUseStatement(sql) {
            bind(it)
            it.statement.execute()
        }
    }

    fun insert(sql: String, bind: Binder.() -> Unit = {}): Long =
        safeUseStatement(sql) {
            bind(it)
            it.statement.executeInsert()
        }

    fun updateDelete(sql: String, bind: Binder.() -> Unit = {}): Int =
        safeUseStatement(sql) {
            bind(it)
            it.statement.executeUpdateDelete()
        }

    fun useStatement(sql: String, block: BinderStatement.() -> Unit) {
        safeUseStatement(sql) {
            block(it)
        }
    }

    fun <T> query(sql: String, bind: Binder.() -> Unit = {}, results: (Iterator<Row>) -> T) {
        safeUseStatement(sql) {
            bind(it)
            val rows = Results(
                it.statement.query()
            )
            results(rows.iterator())
        }
    }

    fun <R> transaction(proc: (DatabaseInstance) -> R): R =
        connection.withTransaction { proc(this) }

    fun close() = cacheLock.withLock {
        closed.value = true
        statementCache.removeAll()
        tryClose()
    }

    fun longForQuery(sql: String): Long = connection.longForQuery(sql)
    fun stringForQuery(sql: String): String = connection.stringForQuery(sql)

    private fun tryClose() {
        try {
            connection.close()
        } catch (e: Exception) {
            //TODO: Need to log this. Should only happen if you're mid-operation.
        }
    }

    private fun <T> safeUseStatement(sql: String, block: (BinderStatement) -> T): T {
        val binderStatement = makeStatement(sql)
        try {
            return block(binderStatement)
        } finally {
            recycle(binderStatement)
        }
    }

    private fun makeStatement(sql: String): BinderStatement = cacheLock.withLock {
        if (closed.value)
            throw IllegalStateException("Cannot use a closed connection")
        statementCache.remove(sql) ?: return@withLock BinderStatement(sql, connection.createStatement(sql))
    }

    internal fun recycle(statement: BinderStatement) = cacheLock.withLock {

        //If connection is closed but an operation is still running, the earlier close call should fail
        //Getting here means the outstanding process finished and now we try closing again
        if (closed.value) {
            statement.statement.finalizeStatement()
            tryClose()
        } else {
            if (cacheSize == 0) {
                statement.statement.finalizeStatement()
            } else {
                statement.reset()
                statementCache.put(statement.sql, statement)
            }
        }
    }
}

fun wrapDatabaseInstance(connection: DatabaseConnection): DatabaseInstance =
    DatabaseInstance(connection, 0)