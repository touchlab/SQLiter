package co.touchlab.sqliter.user

import co.touchlab.sqliter.DatabaseManager
import co.touchlab.stately.collections.frozenLinkedList
import co.touchlab.stately.concurrency.QuickLock
import co.touchlab.stately.concurrency.withLock

class Database(
    private val databaseManager: DatabaseManager,
    private val cacheSize: Int = 200,
    private val instances: Int = 1
) : Operations {

    private val databaseInstances = frozenLinkedList<DatabaseInstance>()

    private val accessLock = QuickLock()
    internal inline fun <R> localInstance(block: DatabaseInstance.() -> R): R {
        val instance: DatabaseInstance = accessLock.withLock {
            if (databaseInstances.size < instances) {
                val inst = DatabaseInstance(databaseManager.createConnection(), cacheSize)
                databaseInstances.add(inst)
                inst
            } else {
                val inst = databaseInstances.removeAt(0)
                databaseInstances.add(inst)
                inst
            }
        }
        try {
            return instance.block()
        } finally {
            accessLock.withLock {
                databaseInstances.add(0, instance)
            }
        }
    }

    /**
     * We don't use the accessLock because it's possible to deadlock. Long story.
     */
    fun close(): Boolean {
        var allClosed = true
        databaseInstances.forEach {
            allClosed = allClosed && it.access { it.close() }
        }
        return allClosed
    }

    override fun execute(sql: String, bind: Binder.() -> Unit) {
        localInstance {
            access { it.execute(sql, bind) }
        }
    }

    override fun insert(sql: String, bind: Binder.() -> Unit): Long = localInstance {
        access {
            it.insert(sql, bind)
        }
    }

    override fun updateDelete(sql: String, bind: Binder.() -> Unit): Int =
        localInstance {
            access { it.updateDelete(sql, bind) }
        }

    override fun useStatement(sql: String, block: BinderStatement.() -> Unit) {
        localInstance {
            access { it.useStatement(sql, block) }
        }
    }

    override fun <T> query(sql: String, bind: Binder.() -> Unit, results: (Iterator<Row>) -> T) {
        //I think we can lift the results out of the access lock, but for simplicity for now,
        //we'll lock it also
        localInstance {
            access {
                it.query(sql, bind, results)
            }
        }
    }

    override fun longForQuery(sql: String): Long = localInstance { access { it.longForQuery(sql) } }
    override fun stringForQuery(sql: String): String = localInstance { access { it.stringForQuery(sql) } }

    fun <R> transaction(proc: (Operations) -> R): R = localInstance { access { it.transaction(proc) } }

    fun <R> instance(proc: (Operations) -> R): R = localInstance { access { proc(it) } }
}

