package co.touchlab.sqliter

import platform.Foundation.NSLock
import kotlin.native.concurrent.AtomicInt

class NativeDatabaseManager(private val path:String,
                            private val configuration: DatabaseConfiguration
                            ):DatabaseManager{
    val lock = NSLock()

    companion object {
        val CREATE_IF_NECESSARY = 0x10000000
    }

    private val connectionCount = AtomicInt(0)

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
            if(connectionCount.value == 0){
                conn.updateJournalMode(configuration.journalMode)
                conn.migrateIfNeeded(configuration.create, configuration.upgrade, configuration.version)
            }
        }finally {
            connectionCount.increment()
            lock.unlock()
        }

        return conn
    }

    internal fun decrementConnectionCount(){
        connectionCount.decrement()
    }

    @SymbolName("Android_Database_SQLiteConnection_nativeOpen")
    private external fun nativeOpen(path:String, openFlags:Int, label:String,
                                    enableTrace:Boolean, enableProfile:Boolean,
                                    lookasideSlotSize:Int, lookasideSlotCount:Int, busyTimeout:Int):Long
}
