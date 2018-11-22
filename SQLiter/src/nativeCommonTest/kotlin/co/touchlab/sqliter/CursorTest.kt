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

import kotlin.test.AfterEach
import kotlin.test.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals

class CursorTest{
    @BeforeEach
    fun before(){
        deleteDatabase("testdb")
    }

    @AfterEach
    fun after(){
        deleteDatabase("testdb")
    }

    @Test
    fun iterator(){
        val manager = createDatabaseManager(DatabaseConfiguration(name = "testdb", version = 1,
            journalMode = JournalMode.WAL, create = { db ->
                db.withStatement(TWO_COL) {
                    execute()
                }
            }))

        val connection = manager.createConnection()
        connection.withStatement("insert into test(num, str)values(?,?)"){
            bindLong(1, 2)
            bindString(2, "asdf")
            executeInsert()
            bindLong(1, 3)
            bindString(2, "qwert")
            executeInsert()
        }

        connection.withStatement("select * from test"){
            var rowCount = 0
            query().iterator().forEach {
                if(rowCount == 0){
                    assertEquals(it.values.get(0).second as Long, 2)
                    assertEquals(it.values.get(1).second as String, "asdf")
                }else if(rowCount == 0){
                    assertEquals(it.values.get(0).second as Long, 3)
                    assertEquals(it.values.get(1).second as String, "qwert")
                }

                rowCount++
            }

            assertEquals(2, rowCount)
        }

        connection.close()
    }
}