package co.touchlab.sqliter

import co.touchlab.stately.collections.AbstractSharedLinkedList
import co.touchlab.stately.collections.frozenLinkedList
import platform.Foundation.NSLock

class NativeDatabaseManager(private val path:String,
                            private val configuration: DatabaseConfiguration
                            ):DatabaseManager{
    val lock = NSLock()

    companion object {
        val CREATE_IF_NECESSARY = 0x10000000
    }

    private val connectionList = frozenLinkedList<NativeDatabaseConnection>(stableIterator = true) as AbstractSharedLinkedList<NativeDatabaseConnection>

    override fun createConnection(): DatabaseConnection {
        lock.lock()

        val conn = NativeDatabaseConnection(this, nativeOpen(
            path,
            CREATE_IF_NECESSARY,
            "asdf",
            false,
            false,
            -1,
            -1,
            configuration.busyTimeout
        ))

        try {
            if(connectionList.size == 0){
                conn.updateJournalMode(configuration.journalMode)
                val walAutocheckpoint = configuration.walAutocheckpoint
                if(walAutocheckpoint != null)
                {
                    val updated = conn.updateWalAutocheckpoint(walAutocheckpoint)
                    if(walAutocheckpoint != updated)
                        throw IllegalStateException("values not equal")
                }
                conn.migrateIfNeeded(configuration.create, configuration.upgrade, configuration.version)
            }
        }finally {
            lock.unlock()
        }

        val node = connectionList.addNode(conn)
        conn.meNode.value = node
        return conn
    }

    override fun close() {
        val iterator = connectionList.iterator()
        iterator.forEach { it.close() }
    }

    @SymbolName("Android_Database_SQLiteConnection_nativeOpen")
    private external fun nativeOpen(path:String, openFlags:Int, label:String,
                                    enableTrace:Boolean, enableProfile:Boolean,
                                    lookasideSlotSize:Int, lookasideSlotCount:Int, busyTimeout:Int):Long
}
