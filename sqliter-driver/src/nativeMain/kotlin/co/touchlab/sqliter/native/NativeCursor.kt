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
import co.touchlab.sqliter.FieldType

class NativeCursor(override val statement: NativeStatement) : Cursor {
    override fun next(): Boolean {
        return statement.sqliteStatement.step()
    }
    override fun isNull(index: Int): Boolean = statement.sqliteStatement.isNull(index)
    override fun getString(index: Int): String = statement.sqliteStatement.columnGetString(index)
    override fun getLong(index: Int): Long = statement.sqliteStatement.columnGetLong(index)
    override fun getBytes(index: Int): ByteArray = statement.sqliteStatement.columnGetBlob(index)
    override fun getDouble(index: Int): Double = statement.sqliteStatement.columnGetDouble(index)
    override fun getType(index: Int): FieldType = FieldType.forCode(statement.sqliteStatement.columnType(index))
    override val columnCount: Int
        get() = statement.sqliteStatement.columnCount()

    override fun columnName(index: Int): String = statement.sqliteStatement.columnName(index)

    override val columnNames: Map<String, Int> by lazy {
        val map = HashMap<String, Int>(this.columnCount)
        for (i in 0 until columnCount) {
            val key = columnName(i)
            if (map.containsKey(key)) {
                var index = 1
                val basicKey = "$key&JOIN"
                var finalKey = basicKey + index
                while (map.containsKey(finalKey)) {
                    finalKey = basicKey + ++index
                }
                map[finalKey] = i
            } else {
                map[key] = i
            }
        }
        map
    }
}
