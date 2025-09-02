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

import co.touchlab.sqliter.Cursor
import co.touchlab.sqliter.Statement
import co.touchlab.sqliter.interop.*

class NativeStatement internal constructor(
    internal val connection: NativeDatabaseConnection,
    internal val sqliteStatement: SqliteStatement,
    sql: String
) : Statement {
    private val logger = connection.dbManager.configuration.loggingConfig.logger
    private val logName:String by lazy { sql.take(40) }
    override fun execute() {
        try {
            logger.v { "execute() on statement '$logName'" }
            sqliteStatement.execute()
        } finally {
            resetStatement()
            clearBindings()
        }
    }

    override fun executeInsert(): Long = try {
        logger.v { "executeInsert() on statement '$logName'" }
        sqliteStatement.executeForLastInsertedRowId()
    } finally {
        resetStatement()
        clearBindings()
    }

    override fun executeUpdateDelete(): Int = try {
        logger.v { "executeUpdateDelete() on statement '$logName'" }
        sqliteStatement.executeForChangedRowCount()
    } finally {
        resetStatement()
        clearBindings()
    }

    override fun query(): Cursor {
        logger.v { "query() on statement '$logName'" }
        return NativeCursor(this)
    }

    override fun finalizeStatement() {
        logger.v { "finalizeStatement() on statement '$logName'" }
        sqliteStatement.finalizeStatement()
    }

    override fun resetStatement() {
        logger.v { "resetStatement() on statement '$logName'" }
        sqliteStatement.resetStatement()
    }

    override fun clearBindings() {
        sqliteStatement.clearBindings()
    }

    override fun bindNull(index: Int) {
        sqliteStatement.bindNull(index)
    }

    override fun bindLong(index: Int, value: Long) {
        sqliteStatement.bindLong(index, value)
    }

    override fun bindDouble(index: Int, value: Double) {
        sqliteStatement.bindDouble(index, value)
    }

    override fun bindString(index: Int, value: String) {
        sqliteStatement.bindString(index, value)
    }

    override fun bindBlob(index: Int, value: ByteArray) {
        sqliteStatement.bindBlob(index, value)
    }

    override fun bindParameterIndex(paramName: String): Int {
        val index = sqliteStatement.bindParameterIndex(paramName)
        if (index == 0)
            throw IllegalArgumentException("Statement parameter $paramName not found")
        return index
    }
}
