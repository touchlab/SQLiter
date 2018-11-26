package co.touchlab.sqlager.user

import co.touchlab.sqliter.DatabaseManager
import co.touchlab.stately.annotation.ThreadLocal
import co.touchlab.stately.collections.AbstractSharedLinkedList
import co.touchlab.stately.collections.SharedLinkedList
import co.touchlab.stately.collections.frozenLinkedList
import co.touchlab.stately.concurrency.ReentrantLock
import co.touchlab.stately.concurrency.ThreadLocalRef
import co.touchlab.stately.concurrency.withLock

class Database(
    private val databaseManager: DatabaseManager,
    private val cacheSize: Int = 200,
    instances: Int = 1
) : Operations {

    //In-memory databases throw rather than wait if another transaction is happening.
    internal val instanceCap = if (databaseManager.configuration.inMemory) {
        1
    } else {
        instances
    }
    internal val databaseInstances = frozenLinkedList<DatabaseInstance>() as SharedLinkedList<DatabaseInstance>

    private val accessLock = ReentrantLock()
    private val threadLocalDatabaseInstance = ThreadLocalRef<DatabaseInstance>()

    /**
     * Gets a db instance from the queue. If we're nesting calls, but the user is calling from the outer db,
     * return the previously associated instance.
     */
    internal inline fun <R> localInstance(block: DatabaseInstance.() -> R): R {
        val localInstance = threadLocalDatabaseInstance.value
        if (localInstance != null) {
            return localInstance.block()
        } else {
            val instanceNode: AbstractSharedLinkedList.Node<DatabaseInstance> = accessLock.withLock {

                if (databaseInstances.size < instanceCap) {
                    val connection = databaseManager.createConnection()
                    val inst = DatabaseInstance(connection, cacheSize)
                    databaseInstances.addNode(inst)
                } else {
                    val node = databaseInstances.nodeIterator().next()
                    node.readd()
                    node
                }

            }

            threadLocalDatabaseInstance.value = instanceNode.nodeValue

            try {
                return instanceNode.nodeValue.block()
            } finally {
                threadLocalDatabaseInstance.value = null
                accessLock.withLock {
                    //                instanceNode.remove()
                    //                databaseInstances.add(0, instanceNode.nodeValue)
                }
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

