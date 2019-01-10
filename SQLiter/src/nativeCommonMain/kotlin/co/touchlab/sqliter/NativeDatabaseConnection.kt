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

import co.touchlab.stately.concurrency.Lock
import co.touchlab.stately.concurrency.withLock
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze

class NativeDatabaseConnection(
    private val dbManager: NativeDatabaseManager,
    connectionPtrArg: Long
) : NativePointer(connectionPtrArg), DatabaseConnection {
    override val closed: Boolean
        get() = pointerClosed

    private val transLock = Lock()


    internal val transaction = AtomicReference<Transaction?>(null)

    data class Transaction(val successful: Boolean)

    override fun createStatement(sql: String): Statement {
        val statementPtr = nativePrepareStatement(nativePointer, sql)
        val statement = NativeStatement(this, statementPtr)

        return statement
    }

    override fun beginTransaction() = transLock.withLock {
        withStatement("BEGIN;") { execute() }
        transaction.value = Transaction(false).freeze()
    }

    override fun setTransactionSuccessful() = transLock.withLock {
        val trans = checkFailTransaction()
        transaction.value = trans.copy(successful = true).freeze()
    }

    override fun endTransaction() = transLock.withLock {
        val trans = checkFailTransaction()

        try {
            withStatement(
            if(trans.successful){
                    "COMMIT;"
            } else {
                    "ROLLBACK;"
            }
            ) { execute() }
        } finally {
            transaction.value = null
        }
    }

    private fun checkFailTransaction(): Transaction {
        return transaction.value ?: throw Exception("No transaction")
    }

    override fun close() {
        closeNativePointer()
    }

    override fun actualClose(nativePointerArg: Long) {
        nativeClose(nativePointerArg) //Call this first, in case it fails
        dbManager.decrementConnectionCount()
    }

    fun migrateIfNeeded(
        create: (DatabaseConnection) -> Unit,
        upgrade: (DatabaseConnection, Int, Int) -> Unit,
        version: Int
    ) {
        val initialVersion = getVersion()
        if (initialVersion == 0) {
            this.withTransaction {
                create(this)
                setVersion(version)
            }
        } else if(initialVersion != version) {
            if(initialVersion > version)
                throw IllegalStateException("Database version $initialVersion newer than config version $version")

            this.withTransaction {
                upgrade(this, initialVersion, version)
                setVersion(version)
            }
        }
    }
}

@SymbolName("SQLiter_SQLiteConnection_nativePrepareStatement")
private external fun nativePrepareStatement(connectionPtr: Long, sql: String): Long

@SymbolName("SQLiter_SQLiteConnection_nativeClose")
private external fun nativeClose(connectionPtr: Long)

