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
import co.touchlab.sqliter.concurrency.Lock
import co.touchlab.sqliter.concurrency.withLock
import co.touchlab.sqliter.interop.SqliteDatabase
import co.touchlab.sqliter.interop.SqliteDatabasePointer
import co.touchlab.sqliter.util.maybeFreeze
import kotlin.concurrent.AtomicInt
import kotlin.concurrent.AtomicReference

class NativeDatabaseConnection internal constructor(
    val dbManager: NativeDatabaseManager,
    private val sqliteDatabase: SqliteDatabase
) : DatabaseConnection {

    private val transLock = Lock()

    private val transaction = AtomicReference<Transaction?>(null)
    private val closedFlag = AtomicInt(0)

    data class Transaction(val successful: Boolean)

    override fun rawExecSql(sql: String) {
        sqliteDatabase.rawExecSql(sql)
    }

    override fun createStatement(sql: String): Statement {
        val statementPtr = sqliteDatabase.prepareStatement(sql)
        val statement = NativeStatement(this, statementPtr, sql)

        return statement
    }

    override fun beginTransaction() = transLock.withLock {
        withStatement("BEGIN;") { execute() }
        transaction.value = Transaction(false).maybeFreeze()
    }

    override fun setTransactionSuccessful() = transLock.withLock {
        val trans = checkFailTransaction()
        transaction.value = trans.copy(successful = true).maybeFreeze()
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
        closedFlag.value = 1
        sqliteDatabase.close()
        dbManager.closeConnection(this)
    }

    override val closed: Boolean
        get() = closedFlag.value != 0

    override fun getDbPointer(): SqliteDatabasePointer = sqliteDatabase.dbPointer

    fun migrateIfNeeded(
        create: (DatabaseConnection) -> Unit,
        upgrade: (DatabaseConnection, Int, Int) -> Unit,
        version: Int
    ) {
        this.withTransaction {
            val initialVersion = getVersion()
            if (initialVersion == 0) {
                create(this)
                setVersion(version)
            } else if (initialVersion != version) {
                if (initialVersion > version)
                    throw IllegalStateException("Database version $initialVersion newer than config version $version")

                upgrade(this, initialVersion, version)
                setVersion(version)
            }
        }
    }
}
