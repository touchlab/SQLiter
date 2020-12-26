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
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.freeze

class NativeDatabaseManager(private val path:String,
                            override val configuration: DatabaseConfiguration
): DatabaseManager {
    override fun createMultiThreadedConnection(): DatabaseConnection {
        return ConcurrentDatabaseConnection(createConnection()).freeze()
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
                -1,
                -1,
                configuration.extendedConfig.busyTimeout,
                configuration.loggingConfig.logger,
                configuration.loggingConfig.verboseDataCalls
            )
            val conn = NativeDatabaseConnection(this, connectionPtrArg)
            configuration.lifecycleConfig.onCreateConnection(conn)

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

            if(configuration.foreignKeyConstraints){
                conn.updateForeignKeyConstraints(true)
            }

            if(newConnection.value == 0){
                conn.updateJournalMode(configuration.typeConfig.journalMode)

                try {
                    conn.migrateIfNeeded(configuration.create, configuration.upgrade, configuration.version)
                } catch (e: Exception) {

                    // If this failed, we have to close the connection or we will end up leaking it.
                    println("attempted to run migration and failed. closing connection.")
                    conn.close()
                    throw e
                }
                newConnection.increment()
            }

            conn
        }
    }

    internal fun closeConnection(connection:DatabaseConnection){
        configuration.lifecycleConfig.onCloseConnection(connection)
    }
}
