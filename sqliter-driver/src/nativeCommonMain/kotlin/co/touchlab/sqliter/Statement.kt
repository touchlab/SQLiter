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

interface Statement{
    fun execute()
    fun executeInsert():Long
    fun executeUpdateDelete():Int
    fun query():Cursor
    fun finalizeStatement()
    fun resetStatement()
    fun clearBindings()
    fun bindNull(index:Int)
    fun bindLong(index:Int, value:Long)
    fun bindDouble(index:Int, value:Double)
    fun bindString(index:Int, value:String)
    fun bindBlob(index:Int, value:ByteArray)
    fun bindParameterIndex(paramName:String):Int
}

fun Statement.bindLong(index:Int, value:Long?){
    if(value == null)
        bindNull(index)
    else
        bindLong(index, value)
}

fun Statement.bindDouble(index:Int, value:Double?){
    if(value == null)
        bindNull(index)
    else
        bindDouble(index, value)
}

fun Statement.bindString(index:Int, value:String?){
    if(value == null)
        bindNull(index)
    else
        bindString(index, value)
}

fun Statement.bindBlob(index:Int, value:ByteArray?){
    if(value == null)
        bindNull(index)
    else
        bindBlob(index, value)
}

fun Statement.longForQuery():Long{
    try {
        val query = query()
        query.next()
        return query.getLong(0)
    } finally {
        resetStatement()
    }
}

fun Statement.stringForQuery():String{
    try {
        val query = query()
        query.next()
        return query.getString(0)
    } finally {
        resetStatement()
    }
}
