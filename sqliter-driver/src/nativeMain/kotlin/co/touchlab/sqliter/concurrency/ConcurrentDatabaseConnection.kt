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

package co.touchlab.sqliter.concurrency

import co.touchlab.sqliter.Cursor
import co.touchlab.sqliter.DatabaseConnection
import co.touchlab.sqliter.FieldType
import co.touchlab.sqliter.Statement
import co.touchlab.sqliter.interop.SqliteDatabasePointer

internal class ConcurrentDatabaseConnection(private val delegateConnection: DatabaseConnection) : DatabaseConnection {
    private val accessLock = Lock()

    override fun rawExecSql(sql: String) = accessLock.withLock { delegateConnection.rawExecSql(sql) }

    override fun createStatement(sql: String): Statement =
        accessLock.withLock { ConcurrentStatement(delegateConnection.createStatement(sql)) }

    override fun beginTransaction() = accessLock.withLock { delegateConnection.beginTransaction() }

    override fun setTransactionSuccessful() = accessLock.withLock { delegateConnection.setTransactionSuccessful() }

    override fun endTransaction() = accessLock.withLock { delegateConnection.endTransaction() }

    override fun close() = accessLock.withLock { delegateConnection.close() }

    override val closed: Boolean
        get() = delegateConnection.closed

    override fun getDbPointer(): SqliteDatabasePointer = delegateConnection.getDbPointer()

    inner class ConcurrentCursor(private val delegateCursor: Cursor) : Cursor {
        override fun next(): Boolean = accessLock.withLock { delegateCursor.next() }

        override fun isNull(index: Int): Boolean = accessLock.withLock { delegateCursor.isNull(index) }

        override fun getString(index: Int): String = accessLock.withLock { delegateCursor.getString(index) }

        override fun getLong(index: Int): Long = accessLock.withLock { delegateCursor.getLong(index) }

        override fun getBytes(index: Int): ByteArray = accessLock.withLock { delegateCursor.getBytes(index) }

        override fun getDouble(index: Int): Double = accessLock.withLock { delegateCursor.getDouble(index) }

        override fun getType(index: Int): FieldType = accessLock.withLock { delegateCursor.getType(index) }

        override val columnCount: Int
            get() = accessLock.withLock { delegateCursor.columnCount }

        override fun columnName(index: Int): String = accessLock.withLock { delegateCursor.columnName(index) }

        override val columnNames: Map<String, Int>
            get() = accessLock.withLock { delegateCursor.columnNames }
        override val statement: Statement
            get() = accessLock.withLock { delegateCursor.statement }

    }

    inner class ConcurrentStatement(internal val delegateStatement: Statement) : Statement {
        override fun execute() = accessLock.withLock { delegateStatement.execute() }

        override fun executeInsert(): Long = accessLock.withLock { delegateStatement.executeInsert() }

        override fun executeUpdateDelete(): Int = accessLock.withLock { delegateStatement.executeUpdateDelete() }

        override fun query(): Cursor = accessLock.withLock { ConcurrentCursor(delegateStatement.query()) }

        override fun finalizeStatement() = accessLock.withLock { delegateStatement.finalizeStatement() }

        override fun resetStatement() = accessLock.withLock { delegateStatement.resetStatement() }

        override fun clearBindings() = accessLock.withLock { delegateStatement.clearBindings() }

        override fun bindNull(index: Int) = accessLock.withLock { delegateStatement.bindNull(index) }

        override fun bindLong(index: Int, value: Long) =
            accessLock.withLock { delegateStatement.bindLong(index, value) }

        override fun bindDouble(index: Int, value: Double) =
            accessLock.withLock { delegateStatement.bindDouble(index, value) }

        override fun bindString(index: Int, value: String) =
            accessLock.withLock { delegateStatement.bindString(index, value) }

        override fun bindBlob(index: Int, value: ByteArray) =
            accessLock.withLock { delegateStatement.bindBlob(index, value) }

        override fun bindParameterIndex(paramName: String): Int =
            accessLock.withLock { delegateStatement.bindParameterIndex(paramName) }
    }
}