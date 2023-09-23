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

package co.touchlab.sqliter.native

import co.touchlab.sqliter.*
import co.touchlab.sqliter.concurrency.ConcurrentDatabaseConnection
import co.touchlab.sqliter.concurrency.Lock
import co.touchlab.sqliter.concurrency.SingleThreadDatabaseConnection
import co.touchlab.sqliter.concurrency.withLock
import co.touchlab.sqliter.interop.OpenFlags
import co.touchlab.sqliter.interop.dbOpen
import co.touchlab.sqliter.util.maybeFreeze
import kotlin.concurrent.AtomicInt

class NativeDatabaseManager(private val path:String,
                            override val configuration: DatabaseConfiguration
): DatabaseManager {
    override fun createMultiThreadedConnection(): DatabaseConnection {
        return ConcurrentDatabaseConnection(createConnection()).maybeFreeze()
    }

    override fun createSingleThreadedConnection(): DatabaseConnection {
        return SingleThreadDatabaseConnection(createConnection())
    }

    private val lock = Lock()

    private val newConnection = AtomicInt(0)

    private fun createConnection(): DatabaseConnection {
        return lock.withLock {
            val connectionPtrArg = dbOpen(
                path,
                listOf(OpenFlags.CREATE_IF_NECESSARY),
                "sqliter",
                false,
                false,
                configuration.extendedConfig.lookasideSlotSize,
                configuration.extendedConfig.lookasideSlotCount,
                configuration.extendedConfig.busyTimeout,
                configuration.loggingConfig.logger,
                configuration.loggingConfig.verboseDataCalls
            )
            val conn = NativeDatabaseConnection(this, connectionPtrArg)
            configuration.lifecycleConfig.onCreateConnection(conn)

            if (configuration.extendedConfig.synchronousFlag != null) {
                conn.updateSynchronousFlag(configuration.extendedConfig.synchronousFlag)
            }

            if (configuration.encryptionConfig.rekey == null) {
                configuration.encryptionConfig.key?.let { conn.setCipherKey(it) }
            } else {
                if (configuration.encryptionConfig.key == null) {
                    // If executed here, it indicate that setCipherKey to `rekey` due to the old key is not set yet.
                    conn.setCipherKey(configuration.encryptionConfig.rekey)
                } else {
                    conn.resetCipherKey(configuration.encryptionConfig.key, configuration.encryptionConfig.rekey)
                }
            }

            // These flags should be explicitly set on each connection at all times.
            //
            // "should set the foreign key enforcement flag [...] and not depend on the default setting."
            // https://www.sqlite.org/pragma.html#pragma_foreign_keys
            // "Recursive triggers may be turned on by default in future versions of SQLite."
            // https://www.sqlite.org/pragma.html#pragma_recursive_triggers
            conn.updateForeignKeyConstraints(configuration.extendedConfig.foreignKeyConstraints)
            conn.updateRecursiveTriggers(configuration.extendedConfig.recursiveTriggers)


            if(newConnection.value == 0){
                conn.updateJournalMode(configuration.journalMode)

                try {
                    val version = configuration.version
                    if(version != NO_VERSION_CHECK)
                        conn.migrateIfNeeded(configuration.create, configuration.upgrade, version)
                } catch (e: Exception) {

                    // If this failed, we have to close the connection or we will end up leaking it.
                    println("attempted to run migration and failed. closing connection.")
                    conn.close()
                    throw e
                }

                // "Temporary" and "purely in-memory" databases live only as long
                // as the connection. Subsequent connections (even if open at
                // the same time) are completely separate databases.
                //
                // If this is the case, do not increment newConnection so that
                // this if block executes on every new connection (i.e. every new
                // ephemeral database).
                when (path) {
                    "", ":memory:" -> {}
                    else -> newConnection.increment()
                }
            }

            conn
        }
    }

    internal fun closeConnection(connection:DatabaseConnection){
        configuration.lifecycleConfig.onCloseConnection(connection)
    }
}

fun AtomicInt.increment() {
    incrementAndGet()
}
