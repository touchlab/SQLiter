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

import kotlin.test.*

class DatabaseConfigurationTest : BaseDatabaseTest(){

    @Test
    fun pathTest(){
        val dbPathString = NativeFileContext.databasePath(TEST_DB_NAME, false)
        assertTrue(dbPathString.endsWith(TEST_DB_NAME))
    }

    @Test
    fun memoryPathTest(){
        val dbPathString = NativeFileContext.databasePath(TEST_DB_NAME, true)
        assertEquals("file:$TEST_DB_NAME?mode=memory&cache=shared", dbPathString)
    }

    @Test
    fun validDatabaseName(){
        assertTrue(validDatabaseName.matches("absdf"))
        assertTrue(validDatabaseName.matches("abs4f"))
        assertTrue(validDatabaseName.matches("abWWs4f"))
        assertTrue(validDatabaseName.matches("_-abWWs4f"))
        assertTrue(validDatabaseName.matches("_-ab.WWs4f"))
        assertFalse(validDatabaseName.matches("_-a bWWs4f"))
        assertFalse(validDatabaseName.matches("_-a ~bWWs4f"))
        assertFalse(validDatabaseName.matches("_-a ~bWWs4f"))
    }

    @Test
    fun invalidDatabaseNameFailsConfig() {
        DatabaseConfiguration("asdf", 1, {})
        assertFails { DatabaseConfiguration("as df", 1, {}) }
    }



    @Test
    fun journalModeSetting()
    {
        val manager = createDatabaseManager(DatabaseConfiguration(name = TEST_DB_NAME, version = 1,
            journalMode = JournalMode.WAL, create = { db ->
                db.withStatement(TWO_COL) {
                    execute()
                }
            }))

        val conn = manager.surpriseMeConnection()
        println("tr 0")
        assertEquals(conn.journalMode, JournalMode.WAL)
        println("tr 1")
        println("Update journal to DELETE result ${conn.updateJournalMode(JournalMode.DELETE)}")
        assertEquals(conn.journalMode, JournalMode.DELETE)
        println("Update journal to WAL result ${conn.updateJournalMode(JournalMode.WAL)}")

        conn.withStatement("insert into test(num, str)values(?,?)"){
            bindLong(1, 333)
            bindString(2, "asdf")
            executeInsert()
        }

        println("Update journal to DELETE result ${conn.updateJournalMode(JournalMode.DELETE)}")
        assertEquals(conn.journalMode, JournalMode.DELETE)

        conn.close()

        val manager2 = createDatabaseManager(DatabaseConfiguration(name = TEST_DB_NAME, version = 1,
            journalMode = JournalMode.WAL, create = {
                fail("Same version shouldn't run")
            }))

        val conn2 = manager2.surpriseMeConnection()
        assertEquals(conn2.journalMode, JournalMode.WAL)
        conn2.close()
    }


}