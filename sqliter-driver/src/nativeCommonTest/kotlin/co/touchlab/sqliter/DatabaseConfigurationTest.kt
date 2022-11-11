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
        val dbPathString = DatabaseFileContext.databasePath(TEST_DB_NAME, null)
        assertTrue(dbPathString.endsWith(TEST_DB_NAME))
    }

    @Test
    fun databasePathRemovesExtraSlashes() {
        val dbPathString = DatabaseFileContext.databasePath(TEST_DB_NAME, "//tmp//")
        assertEquals("/tmp/$TEST_DB_NAME", dbPathString)
    }

    @Test
    fun databasePathRemovesFileUrlPrefix() {
        val dbPathString = DatabaseFileContext.databasePath(TEST_DB_NAME, "file:///tmp/")
        assertEquals("/tmp/$TEST_DB_NAME", dbPathString)
    }

    @Test
    fun databasePathRemovesFileUrlPrefixInCaps() {
        val dbPathString = DatabaseFileContext.databasePath(TEST_DB_NAME, "FILE:///tmp/")
        assertEquals("/tmp/$TEST_DB_NAME", dbPathString)
    }

    @Test
    fun memoryOnlyTest(){
        val conf = DatabaseConfiguration(
            name = null,
            inMemory = true,
            version = 1, create = { db ->
            db.withStatement(TWO_COL) {
                execute()
            }
        })
        val dbPathString = diskOrMemoryPath(conf)
        assertEquals(":memory:", dbPathString)
    }

    @Test
    fun memoryPathTest(){
        val conf = DatabaseConfiguration(
            name = TEST_DB_NAME,
            inMemory = true,
            version = 1, create = { db ->
            db.withStatement(TWO_COL) {
                execute()
            }
        })
        val dbPathString = diskOrMemoryPath(conf)
        assertEquals("file:$TEST_DB_NAME?mode=memory&cache=shared", dbPathString)
    }

    fun checkFilePath(name: String, path: String?) {
        var conn: DatabaseConnection? = null
        val config = DatabaseConfiguration(
            name = name,
            extendedConfig = DatabaseConfiguration.Extended(basePath = path),
            version = 1, create = { db ->
            db.withStatement(TWO_COL) {
                execute()
            }
        })

        try {
            val expectedPath = DatabaseFileContext.databaseFile(name, path)
            val manager = createDatabaseManager(config)

            conn = manager.createMultiThreadedConnection()

            assertTrue(expectedPath.exists())
        } finally {
            conn?.close()
            DatabaseFileContext.deleteDatabase(config)
        }
    }

    @Test
    fun noSlashInName(){
        assertFails {
            checkFilePath("arst/qwfp", null)
        }
    }

    @Test
    fun basicDbNameWorks(){
        checkFilePath("arst", null)
    }

    @Test
    fun nameWithSpace(){
        checkFilePath("ar st", null)
    }

    /*@Test
    fun configConnection(){
        var called = false
        val config = DatabaseConfiguration(name = "configConnection", version = 1, create = { _ -> },
            lifecycleConfig = DatabaseConfiguration.Lifecycle(
                onCreateConnection = {conn ->
                    assertNotNull(conn)
                    called = true
                }
            ),
         )

        var conn: DatabaseConnection? = null
        try {
            val manager = createDatabaseManager(config)
            conn = manager.createMultiThreadedConnection()

            assertTrue(called)
        } finally {
            conn?.close()
            DatabaseFileContext.deleteDatabase(config)
        }
    }
*/
    @Test
    fun journalModeSetting()
    {
        val manager = createDatabaseManager(DatabaseConfiguration(name = TEST_DB_NAME, version = 1,
            journalMode = JournalMode.WAL,
            create = { db ->
                db.withStatement(TWO_COL) {
                    execute()
                }
            }))

        val conn = manager.surpriseMeConnection()

        assertEquals(conn.journalMode, JournalMode.WAL)

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

    @Test
    fun foreignKeyConstraintsSetting(){
        runFkTest("fkoff", false)
        runFkTest("fkon", true)
    }

    @Test
    fun noVersionTest(){
        val conf = DatabaseConfiguration(
            name = TEST_DB_NAME,
            inMemory = true,
            version = NO_VERSION_CHECK,
            create = { throw IllegalStateException("Shouldn't be here") })
        val manager = createDatabaseManager(conf)
        val conn = manager.createMultiThreadedConnection()
        assertEquals(conn.getVersion(), 0)
    }

    private fun runFkTest(dbname: String, enableFK: Boolean){
        var bookId = 1
        fun makeBookWithoutTransaction(conn: DatabaseConnection) =
            conn.withStatement("insert into book(id, name, author_id)values(${bookId++}, 'Hello Book', 5)") { executeInsert() }

        fun makeBook(conn: DatabaseConnection) {
            conn.withTransaction { conn2 ->
                makeBookWithoutTransaction(conn2)
            }
        }
        fun checkAB(conn:DatabaseConnection, expectBooks: Int, expectAuthors: Int){
            val books = conn.longForQuery("select count(*) from book").toInt()
            val authors = conn.longForQuery("select count(*) from author").toInt()

            assertEquals(expectBooks, books)
            assertEquals(expectAuthors, authors)
        }

        val manager = createDatabaseManager(DatabaseConfiguration(
            name = dbname,
            version = 1,
            journalMode = JournalMode.WAL,
            extendedConfig = DatabaseConfiguration.Extended(foreignKeyConstraints = enableFK),
            create = { db ->
                db.withStatement(AUTHOR) {
                    execute()
                }
                db.withStatement(BOOK) {
                    execute()
                }
            }))

        val conn = manager.createMultiThreadedConnection()

        try {
            checkAB(conn, 0, 0)

            if(enableFK){
                assertFails { makeBook(conn) }
                assertFails { makeBookWithoutTransaction(conn) }
                checkAB(conn, 0, 0)
            }else{
                makeBook(conn)
                makeBookWithoutTransaction(conn)
                checkAB(conn, 2, 0)
            }
        } finally {
            conn.close()
            DatabaseFileContext.deleteDatabase(dbname)
        }
    }

    private val AUTHOR = """CREATE TABLE author (
    id INTEGER NOT NULL PRIMARY KEY,
    name TEXT NOT NULL
    );"""

    private val BOOK = """CREATE TABLE book (
    id INTEGER NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    author_id INTEGER NOT NULL,
    FOREIGN KEY (author_id) REFERENCES author(id) ON DELETE CASCADE
    );"""
}