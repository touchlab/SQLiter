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
import sql.*

class NativeStatement(
    internal val connection: NativeDatabaseConnection,
    internal val sqliteStatement: SqliteStatement
) : Statement {


    override fun execute() {
        try {
            sqliteStatement.execute()
        } finally {
            resetStatement()
            clearBindings()
        }
    }

    override fun executeInsert(): Long = try {
        sqliteStatement.executeForLastInsertedRowId()
    } finally {
        resetStatement()
        clearBindings()
    }

    override fun executeUpdateDelete(): Int = try {
        sqliteStatement.executeForChangedRowCount()
    } finally {
        resetStatement()
        clearBindings()
    }

    override fun query(): Cursor = NativeCursor(this)

    override fun finalizeStatement() {
        sqliteStatement.finalizeStatement()
    }

    override fun resetStatement() {
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
