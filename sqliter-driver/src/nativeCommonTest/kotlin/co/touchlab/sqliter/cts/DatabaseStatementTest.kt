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

package co.touchlab.sqliter.cts

import co.touchlab.sqliter.*
import kotlin.test.*

class DatabaseStatementTest{
    private lateinit var mDatabase:DatabaseConnection

    val isPerformanceOnly:Boolean
        get() {
            return false
        }

    @BeforeTest
    protected fun setUp() {
        DatabaseFileContext.deleteDatabase(DATABASE_NAME)
        mDatabase = createDatabaseManager(DatabaseConfiguration(
            name = DATABASE_NAME,
            version = CURRENT_DATABASE_VERSION,
            create = {}
        )).surpriseMeConnection()
    }

    @AfterTest
    protected fun tearDown() {
        mDatabase.close()
        DatabaseFileContext.deleteDatabase(DATABASE_NAME)
    }

    private fun populateDefaultTable() {
        mDatabase.withStatement("CREATE TABLE test (_id INTEGER PRIMARY KEY, data TEXT);"){execute()}
        mDatabase.withStatement("INSERT INTO test (data) VALUES ('" + sString1 + "');"){executeInsert()}
        mDatabase.withStatement("INSERT INTO test (data) VALUES ('" + sString2 + "');"){executeInsert()}
        mDatabase.withStatement("INSERT INTO test (data) VALUES ('" + sString3 + "');"){executeInsert()}
    }

    @Test
    fun testExecuteStatement() {
        populateDefaultTable()
        val statement = mDatabase.createStatement("DELETE FROM test")
        statement.execute()
        assertEquals(0, mDatabase.longForQuery("select count(*) from test"))
        statement.finalizeStatement()
    }

    @Test
    fun testSimpleQuery() {
        mDatabase.withStatement("CREATE TABLE test (num INTEGER NOT NULL, str TEXT NOT NULL);"){execute()}
        mDatabase.withStatement("INSERT INTO test VALUES (1234, 'hello');"){executeInsert()}
        val statement1 = mDatabase.createStatement("SELECT num FROM test WHERE str = ?")
        val statement2 = mDatabase.createStatement("SELECT str FROM test WHERE num = ?")

        try {
            statement1.bindString(1, "hello")
            assertEquals(1234, statement1.longForQuery())
            statement1.bindString(1, "world")
            //sqlite doesn't fail on cross-type results. We're not either.
//            assertFails { statement1.longForQuery() }

            statement2.bindLong(1, 1234)
            assertEquals("hello", statement2.stringForQuery())
            statement2.bindLong(1, 5678)
            //sqlite doesn't fail on cross-type results. We're not either.
//            assertFails { statement1.stringForQuery() }
        } finally {
            statement1.finalizeStatement()
            statement2.finalizeStatement()
        }
    }

    @Test
    fun testStatementLongBinding() {
        mDatabase.withStatement("CREATE TABLE test (num INTEGER);"){execute()}
        mDatabase.withStatement("INSERT INTO test (num) VALUES (?)") {
            for (i in 0..9) {
                bindLong(1, i.toLong())
                execute()
            }
        }
        cursorCheck {numCol, i, c ->
            val num = c.getLong(numCol)
            assertEquals(i.toLong(), num)
        }
    }

    private fun cursorCheck(block:(Int, Int, Cursor)->Unit){
        mDatabase.withStatement("select * from test"){
            val c = query()
            val numCol = c.getColumnIndexOrThrow("num")
            for (i in 0..9)
            {
                c.next()
                block(numCol, i, c)
            }
        }
    }

    @Test
    fun testStatementStringBinding() {
        mDatabase.withStatement("CREATE TABLE test (num TEXT);"){execute()}
        mDatabase.withStatement("INSERT INTO test (num) VALUES (?)"){
            for (i in 0..9)
            {
                bindString(1, i.toString(16))
                execute()
            }
        }

        cursorCheck {numCol, i, c ->
            val num = c.getString(numCol)
            assertEquals(i.toString(16), num)
        }
    }

    @Test
    fun testStatementClearBindings() {
        mDatabase.withStatement("CREATE TABLE test (num INTEGER);"){execute()}
        mDatabase.withStatement("INSERT INTO test (num) VALUES (?)"){
            for (i in 0..9)
            {
                bindLong(1, i.toLong())
                clearBindings()
                execute()
            }
        }

        cursorCheck {numCol, _, c ->
            assertTrue(c.isNull(numCol))
        }
    }

    /*@Test
    fun testSimpleStringBinding() {
        mDatabase.execSQL("CREATE TABLE test (num TEXT, value TEXT);")
        val statement = "INSERT INTO test (num, value) VALUES (?,?)"
        val args = arrayOfNulls<String>(2)
        for (i in 0..1)
        {
            args[i] = i.toString(16)
        }
        mDatabase.execSQL(statement, args as Array<Any?>)
        val c = mDatabase.query("test", null, null, null, null, null, null)
        val numCol = c.getColumnIndexOrThrow("num")
        val valCol = c.getColumnIndexOrThrow("value")
        c.moveToFirst()
        val num = c.getString(numCol)
        assertEquals("0", num)
        val `val` = c.getString(valCol)
        assertEquals("1", `val`)
        c.close()
    }*/

    @Test
    fun testStatementMultipleBindings() {
        mDatabase.withStatement("CREATE TABLE test (num INTEGER, str TEXT);"){execute()}
        mDatabase.withStatement("INSERT INTO test (num, str) VALUES (?, ?)"){
            for (i in 0..9)
            {
                bindLong(1, i.toLong())
                bindString(2, i.toString(16))
                execute()
            }
        }

        mDatabase.withStatement("select * from test"){
            val c = query()
            val numCol = c.getColumnIndexOrThrow("num")
            val strCol = c.getColumnIndexOrThrow("str")

            for (i in 0..9)
            {
                assertTrue(c.next())
                val num = c.getLong(numCol)
                val str = c.getString(strCol)
                assertEquals(i.toLong(), num)
                assertEquals(i.toString(16), str)
            }
        }
    }

    @Test
    fun testStatementConstraint() {
        mDatabase.withStatement("CREATE TABLE test (num INTEGER NOT NULL);"){execute()}
        mDatabase.withStatement("INSERT INTO test (num) VALUES (?)"){
            assertFails { executeInsert() }
            bindLong(1, 1)
            executeInsert()
        }


        mDatabase.withStatement("select * from test"){
            val c = query()
            val numCol = c.getColumnIndexOrThrow("num")
            c.next()
            val num = c.getLong(numCol)
            assertEquals(1, num)
        }
    }

    companion object {
        private val sString1 = "this is a test"
        private val sString2 = "and yet another test"
        private val sString3 = "this string is a little longer, but still a test"
        private val DATABASE_NAME = "database_test.db"
        private val CURRENT_DATABASE_VERSION = 42
    }
}
