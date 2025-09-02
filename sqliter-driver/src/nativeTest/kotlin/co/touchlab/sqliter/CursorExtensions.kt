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

fun Cursor.iterator():CursorIterator = CursorIterator(this)

class Row{
    val values = mutableListOf<Pair<FieldType, Any?>>()
}

class CursorIterator(private val cursor: Cursor):Iterator<Row> {
    var hadNext = cursor.next()

    override fun hasNext(): Boolean = hadNext

    override fun next(): Row {
        val result = Row()
        for(i in 0 until cursor.columnCount){
            val type = cursor.getType(i)
            val value:Any? = when(type){
                FieldType.TYPE_BLOB -> cursor.getBytes(i)
                FieldType.TYPE_FLOAT -> cursor.getDouble(i)
                FieldType.TYPE_INTEGER -> cursor.getLong(i)
                FieldType.TYPE_NULL -> null
                FieldType.TYPE_TEXT -> cursor.getString(i)
            }

            result.values.add(Pair(type, value))
        }

        hadNext = cursor.next()

        return result
    }
}