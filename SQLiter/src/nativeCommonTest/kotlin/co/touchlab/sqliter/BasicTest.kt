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

import kotlin.system.getTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BasicTest{

    @Test
    fun createTable(){
        basicTestDb {manager ->
            val connection = manager.createSingleThreadedConnection()
            val start = getTimeMillis()
            connection.withTransaction {
                val statement = it.createStatement("INSERT INTO test VALUES (?, ?, ?, ?)")
                for(i in 0 until 100_000) {
                    statement.bindLong(1, i.toLong())
                    statement.bindString(2, "Hilo $i")
                    statement.bindString(3, "asdf jfasdf $i fflkajsdf $i")
                    statement.bindString(4, "WWWWW QWER jfasdf $i fflkajsdf $i")
                    statement.executeInsert()
                    statement.resetStatement()
                }
                statement.finalizeStatement()
            }

            connection.withStatement("SELECT * FROM test") {
                val cursor = query()
                val timeBlocking = timeCursorBlocking(cursor) {
                    it.next()
                }

                println("Query timeBlocking: $timeBlocking")
            }

            println("Full run time ${getTimeMillis() - start}")
            connection.close()
        }
    }

    inline fun timeCursorBlocking(cursor:Cursor,  proc:(Cursor)->Boolean):Long{
        val start = getTimeMillis()
        var rowCount = 0
        while (proc(cursor)) {
            rowCount++
            assertTrue(cursor.getLong(0) > -1)
            assertTrue(cursor.getString(1).isNotEmpty())
            assertTrue(cursor.getString(2).isNotEmpty())
            assertTrue(cursor.getString(3).isNotEmpty())
        }

        val names = cursor.columnNames
        assertEquals(4, names.size)

        return getTimeMillis() - start
    }
}