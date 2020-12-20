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

import sql.*

class NativeStatement(
    internal val connection: NativeDatabaseConnection,
    internal val sqliteStatement: SqliteStatement
) : Statement {


    override fun execute() {
        try {
            sqliteStatement.nativeExecute()
        } finally {
            resetStatement()
            clearBindings()
        }
    }

    override fun executeInsert(): Long = try {
        sqliteStatement.nativeExecuteForLastInsertedRowId()
    } finally {
        resetStatement()
        clearBindings()
    }

    override fun executeUpdateDelete(): Int = try {
        sqliteStatement.nativeExecuteForChangedRowCount()
    } finally {
        resetStatement()
        clearBindings()
    }

    override fun query(): Cursor = NativeCursor(this)

    override fun finalizeStatement() {
        sqliteStatement.nativeFinalizeStatement()
    }

    override fun resetStatement() {
        sqliteStatement.nativeResetStatement()
    }

    override fun clearBindings() {
        sqliteStatement.nativeClearBindings()
    }

    override fun bindNull(index: Int) {
        sqliteStatement.nativeBindNull(index)
    }

    override fun bindLong(index: Int, value: Long) {
        sqliteStatement.nativeBindLong(index, value)
    }

    override fun bindDouble(index: Int, value: Double) {
        sqliteStatement.nativeBindDouble(index, value)
    }

    override fun bindString(index: Int, value: String) {
        sqliteStatement.nativeBindString(index, value)
    }

    override fun bindBlob(index: Int, value: ByteArray) {
        sqliteStatement.nativeBindBlob(index, value)
    }

    override fun bindParameterIndex(paramName: String): Int {
        val index = sqliteStatement.nativeBindParameterIndex(paramName)
        if (index == 0)
            throw IllegalArgumentException("Statement parameter $paramName not found")
        return index
    }
}
