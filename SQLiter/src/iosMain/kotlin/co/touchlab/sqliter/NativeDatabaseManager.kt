package co.touchlab.sqliter

import co.touchlab.stately.collections.AbstractSharedLinkedList
import co.touchlab.stately.collections.frozenLinkedList

class NativeDatabaseManager(private val path:String):DatabaseManager{
    private val connectionList = frozenLinkedList<NativeDatabaseConnection>(stableIterator = true) as AbstractSharedLinkedList<NativeDatabaseConnection>

    override fun createConnection(openFlags: Int): DatabaseConnection {
        val conn = NativeDatabaseConnection(this, nativeOpen(
            path,
            openFlags,
            "asdf",
            false,
            false,
            -1,
            -1
        ))
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
                                    lookasideSlotSize:Int, lookasideSlotCount:Int):Long
}
