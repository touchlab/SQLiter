package co.touchlab.sqliter.user

import co.touchlab.sqliter.*
import co.touchlab.stately.collections.frozenHashMap
import co.touchlab.stately.collections.frozenLinkedList
import co.touchlab.stately.concurrency.AtomicBoolean
import co.touchlab.stately.concurrency.QuickLock
import co.touchlab.stately.concurrency.withLock
import co.touchlab.stately.freeze

class PreparedStatement(private val sql: String, private val databaseConnection: DatabaseConnection):BinderStatementRecycler() {

    private val closed = AtomicBoolean(false)
    //    private val allStatements = frozenLinkedList<BinderStatement>(stableIterator = false)
    private val statementCache = ObjectCache<BinderStatement>()

    private fun makeStatement(): BinderStatement {
        if (closed.value)
            throw IllegalStateException("PreparedStatement closed")

        val statement = statementCache.pop()
        if (statement != null)
            return statement

        val newStatement = BinderStatement(sql, databaseConnection.createStatement(sql))
//        allStatements.add(newStatement)
        return newStatement
    }

    fun execute(bind: Binder.() -> Unit = {}) {
        return bindrun(bind) {
            it.execute()
        }
    }

    fun executeInsert(bind: Binder.() -> Unit = {}): Long {
        return bindrun(bind) {
            it.executeInsert()
        }
    }

    fun executeUpdateDelete(bind: Binder.() -> Unit = {}): Int {
        return bindrun(bind) {
            it.executeUpdateDelete()
        }
    }

    fun query(bind: Binder.() -> Unit = {}): Results {
        val binderStatement = makeStatement()
        try {
            binderStatement.bind()
            return Results(binderStatement.statement.query(), binderStatement, this)
        } catch (e: Exception) {
            recycle(binderStatement)
            throw e
        }
    }

    fun close() {
        closed.value = true
        statementCache.all {
            it.statement.finalizeStatement()
        }
    }

    private inline fun <T> bindrun(bind: Binder.() -> Unit, exe: (Statement) -> T): T {
        val binderStatement = makeStatement()
        try {
            binderStatement.bind()
            return exe(binderStatement.statement)
        } finally {
            binderStatement.statement.resetStatement()
            recycle(binderStatement)
        }
    }

    override fun recycle(binderStatement: BinderStatement) {
        if (closed.value) {
            binderStatement.statement.finalizeStatement()
        } else {
            binderStatement.reset()
            statementCache.push(binderStatement)
        }
    }
}

internal class ObjectCache<T> {
    internal val cacheList = frozenLinkedList<T>(stableIterator = false)
    private val lock = QuickLock()

    internal fun all(block: (T) -> Unit) = lock.withLock {
        cacheList.forEach(block)
    }

    fun push(t: T) = lock.withLock {
        cacheList.add(t)
    }

    fun pop(): T? = lock.withLock {
        return if (cacheList.size == 0)
            null
        else
            cacheList.removeAt(0)
    }
}