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

/**
 * Simplified Cursor implementation. Forward-only traversal.
 */
interface Cursor {
    fun next(): Boolean
    fun isNull(index: Int): Boolean
    fun getString(index: Int): String
    fun getLong(index: Int): Long
    fun getBytes(index: Int): ByteArray
    fun getDouble(index: Int): Double
    fun getType(index: Int):FieldType
    val columnCount: Int
    fun columnName(index: Int): String
    val columnNames: Map<String, Int>
    val statement:Statement
}

enum class FieldType(val nativeCode: Int) {
    //These names a prefixed with 'TYPE_' to avoid Kotlin/Native to Swift name collisions
    TYPE_INTEGER(1), TYPE_FLOAT(2), TYPE_BLOB(4), TYPE_NULL(5), TYPE_TEXT(3);

    companion object {
        fun forCode(nativeCode: Int):FieldType{
            FieldType.values().forEach {
                if(it.nativeCode == nativeCode)
                    return it
            }
            throw IllegalArgumentException("Native code not found $nativeCode")
        }
    }
}

fun Cursor.getStringOrNull(index: Int): String?{
    return if(isNull(index))
        null
    else
        getString(index)
}

fun Cursor.getLongOrNull(index: Int): Long?{
    return if(isNull(index))
        null
    else
        getLong(index)
}

fun Cursor.getBytesOrNull(index: Int): ByteArray?{
    return if(isNull(index))
        null
    else
        getBytes(index)
}

fun Cursor.getDoubleOrNull(index: Int): Double?{
    return if(isNull(index))
        null
    else
        getDouble(index)
}

fun Cursor.forLong():Long{
    next()
    val result = getLong(0)
    statement.resetStatement()
    return result
}

fun Cursor.getColumnIndexOrThrow(name:String):Int = columnNames[name] ?: throw IllegalArgumentException("Col for $name not found")

