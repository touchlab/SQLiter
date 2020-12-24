package co.touchlab.sqlager.user

import co.touchlab.sqliter.DatabaseConnection
import co.touchlab.sqliter.longForQuery
import co.touchlab.sqliter.stringForQuery
import co.touchlab.sqliter.withTransaction
import co.touchlab.stately.collections.frozenLruCache
import co.touchlab.stately.concurrency.AtomicBoolean
import co.touchlab.stately.concurrency.Lock
import co.touchlab.stately.concurrency.withLock

internal class DatabaseInstance internal constructor(
    private val connection: DatabaseConnection,
    private val cacheSize: Int
) : Operations {

    internal val closed = AtomicBoolean(false)
    internal val statementCache = frozenLruCache<String, BinderStatement>(cacheSize) {
        it.value.statement.finalizeStatement()
    }

    private val accessLock = Lock()
    internal inline fun <R> access(block:(DatabaseInstance)->R):R = accessLock.withLock {
        block(this)
    }

    private val cacheLock = Lock()

    override fun execute(sql: String, bind: Binder.() -> Unit) {
        safeUseStatement(sql) {
            bind(it)
            it.statement.execute()
        }
    }

    override fun insert(sql: String, bind: Binder.() -> Unit): Long =
        safeUseStatement(sql) {
            bind(it)
            it.statement.executeInsert()
        }

    override fun updateDelete(sql: String, bind: Binder.() -> Unit): Int =
        safeUseStatement(sql) {
            bind(it)
            it.statement.executeUpdateDelete()
        }

    override fun useStatement(sql: String, block: BinderStatement.() -> Unit) {
        safeUseStatement(sql) {
            block(it)
        }
    }

    override fun <T> query(sql: String, bind: Binder.() -> Unit, results: (Iterator<Row>) -> T) {
        safeUseStatement(sql) {
            bind(it)
            val rows = Results(
                it.statement.query()
            )
            results(rows.iterator())
        }
    }

    fun <R> transaction(proc: (Operations) -> R): R =
        connection.withTransaction { proc(this) }

    fun close():Boolean = cacheLock.withLock {
        closed.value = true
        statementCache.removeAll()
        tryClose()
    }

    override fun longForQuery(sql: String): Long = connection.longForQuery(sql)
    override fun stringForQuery(sql: String): String = connection.stringForQuery(sql)

    private fun tryClose():Boolean {
        return try {
            connection.close()
            true
        } catch (e: Exception) {
            //TODO: Need to log this. Should only happen if you're mid-operation.
            false
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
        statementCache.remove(key = sql, skipCallback = true) ?: return@withLock BinderStatement(
            sql,
            connection.createStatement(sql)
        )
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
                val existing = statementCache.put(statement.sql, statement)
                if(existing != null)
                    existing.statement.finalizeStatement()
                Unit
            }
        }
    }
}

fun wrapDatabaseInstance(connection: DatabaseConnection): Operations =
    DatabaseInstance(connection, 0)