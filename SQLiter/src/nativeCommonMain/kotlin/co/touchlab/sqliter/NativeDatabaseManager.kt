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

import platform.Foundation.NSLock
import kotlin.native.concurrent.AtomicInt

class NativeDatabaseManager(private val path:String,
                            override val configuration: DatabaseConfiguration
                            ):DatabaseManager{
    val lock = NSLock()

    companion object {
        val CREATE_IF_NECESSARY = 0x10000000
    }

    internal val connectionCount = AtomicInt(0)

    override fun createConnection(): DatabaseConnection {
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

            if(connectionCount.value == 0){
                conn.updateJournalMode(configuration.journalMode)
                conn.migrateIfNeeded(configuration.create, configuration.upgrade, configuration.version)
            }

            return conn
        }
        finally {
            connectionCount.increment()
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