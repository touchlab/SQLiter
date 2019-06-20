/*
 * Copyright (C) 2018 Touchlab, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.touchlab.sqliter

import co.touchlab.sqliter.concurrency.ConcurrentDatabaseConnection
import co.touchlab.sqliter.concurrency.SingleThreadDatabaseConnection
import co.touchlab.stately.concurrency.Lock
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.freeze

class NativeDatabaseManager(private val path:String,
                            override val configuration: DatabaseConfiguration
):DatabaseManager{
    override fun createMultiThreadedConnection(): DatabaseConnection {
        return ConcurrentDatabaseConnection(createConnection()).freeze()
    }

    override fun createSingleThreadedConnection(): DatabaseConnection {
        return SingleThreadDatabaseConnection(createConnection())
    }

    val lock = Lock()

    companion object {
        val CREATE_IF_NECESSARY = 0x10000000
    }

    internal val connectionCount = AtomicInt(0)

    private fun createConnection(): DatabaseConnection {
        lock.lock()

        try {
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

            if(configuration.foreignKeyConstraints){
                conn.updateForeignKeyConstraints(true)
            }

            if(connectionCount.value == 0){
                conn.updateJournalMode(configuration.journalMode)

                try {
                    conn.migrateIfNeeded(configuration.create, configuration.upgrade, configuration.version)
                } catch (e: Exception) {

                    // If this failed, we have to close the connection or we will end up leaking it.
                    println("attempted to run migration and failed. closing connection.")
                    conn.close()
                    throw e
                }
            }

            connectionCount.increment()

            return conn
        }
        finally {
            lock.unlock()
        }
    }

    internal fun decrementConnectionCount(){
        connectionCount.decrement()
    }
}

@SymbolName("SQLiter_SQLiteConnection_nativeOpen")
private external fun nativeOpen(path:String, openFlags:Int, label:String,
                                enableTrace:Boolean, enableProfile:Boolean,
                                lookasideSlotSize:Int, lookasideSlotCount:Int, busyTimeout:Int):Long