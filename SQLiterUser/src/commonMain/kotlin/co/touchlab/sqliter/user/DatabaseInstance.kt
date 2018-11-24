package co.touchlab.sqliter.user

import co.touchlab.sqliter.DatabaseConnection
import co.touchlab.sqliter.withTransaction
import co.touchlab.stately.collections.frozenLruCache
import co.touchlab.stately.concurrency.QuickLock
import co.touchlab.stately.concurrency.withLock

class DatabaseInstance internal constructor(private val connection: DatabaseConnection, cacheSize:Int = 200):
BinderStatementRecycler(){
    override fun recycle(statement: BinderStatement) {
        statementCache.put(statement.sql, statement)
    }

    private val statementCache = frozenLruCache<String, BinderStatement>(cacheSize){
        it.value.statement.finalizeStatement()
    }

    private val cacheLock = QuickLock()

    fun statement(sql:String):PreparedStatement{
        return PreparedStatement(sql, connection)
    }

    internal fun cachedStatement(sql:String):BinderStatement{
        return makeStatement(sql)
    }

    fun execute(sql:String, bind:Binder.()->Unit = {}) {
        val makeStatement = makeStatement(sql)
        bind(makeStatement)
        makeStatement.statement.execute()
    }

    fun executeInsert(sql:String, bind: Binder.() -> Unit = {}): Long {
        val makeStatement = makeStatement(sql)
        bind(makeStatement)
        return makeStatement.statement.executeInsert()
    }

    fun executeUpdateDelete(sql:String, bind: Binder.() -> Unit = {}): Int {
        val makeStatement = makeStatement(sql)
        bind(makeStatement)
        return makeStatement.statement.executeUpdateDelete()
    }

    fun query(sql:String, bind:Binder.()->Unit = {}):Results {
        val makeStatement = makeStatement(sql)
        bind(makeStatement)
        return Results(makeStatement.statement.query(),
            makeStatement,
            this)
    }

    fun <R> transaction(proc: (DatabaseInstance) -> R): R =
        connection.withTransaction { proc(this) }

    fun close(){
        connection.close()
    }

    private fun makeStatement(sql: String):BinderStatement = cacheLock.withLock{
        statementCache.remove(sql) ?: return@withLock BinderStatement(sql, connection.createStatement(sql))
    }
}

fun wrapDatabaseInstance(connection: DatabaseConnection):DatabaseInstance =
        DatabaseInstance(connection, 1)