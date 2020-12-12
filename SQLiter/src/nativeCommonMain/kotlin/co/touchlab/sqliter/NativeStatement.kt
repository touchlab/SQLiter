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
    nativePointer:SqliteStatement):NativePointer<SqliteStatement>(nativePointer), Statement {
    

    override fun execute() {
        try {
            nativeExecute(connection.nativePointer, nativePointer)
        } finally {
            resetStatement()
            clearBindings()
        }
    }

    override fun executeInsert():Long = try {
        nativeExecuteForLastInsertedRowId(connection.nativePointer, nativePointer)
    } finally {
        resetStatement()
        clearBindings()
    }

    override fun executeUpdateDelete():Int = try {
        nativeExecuteForChangedRowCount(connection.nativePointer, nativePointer)
    } finally {
        resetStatement()
        clearBindings()
    }

    override fun query(): Cursor = NativeCursor(this)

    override fun finalizeStatement() {
        closeNativePointer()
    }

    override fun actualClose(nativePointerArg: SqliteStatement) {
        nativeFinalizeStatement(connection.nativePointer, nativePointerArg)
    }

    override fun resetStatement() {
        nativeResetStatement(connection.nativePointer, nativePointer)
    }

    override fun clearBindings() {
        nativeClearBindings(connection.nativePointer, nativePointer)
    }

    override fun bindNull(index: Int) {
        nativeBindNull(connection.nativePointer, nativePointer, index)
    }

    override fun bindLong(index: Int, value: Long) {
        nativeBindLong(connection.nativePointer, nativePointer, index, value)
    }

    override fun bindDouble(index: Int, value: Double) {
        nativeBindDouble(connection.nativePointer, nativePointer, index, value)
    }

    override fun bindString(index: Int, value: String) {
        nativeBindString(connection.nativePointer, nativePointer, index, value)
    }

    override fun bindBlob(index: Int, value: ByteArray) {
        nativeBindBlob(connection.nativePointer, nativePointer, index, value)
    }

    override fun bindParameterIndex(paramName: String): Int {
        val index = nativeBindParameterIndex(nativePointer, paramName)
        if(index == 0)
            throw IllegalArgumentException("Statement parameter $paramName not found")
        return index
    }
}
