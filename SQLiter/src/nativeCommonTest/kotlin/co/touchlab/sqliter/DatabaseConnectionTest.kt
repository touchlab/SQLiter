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

import co.touchlab.sqliter.NativeFileContext.deleteDatabase
import kotlin.test.*

class DatabaseConnectionTest {
    @Test
    fun secondTransactionFailsRetainsExistingTransaction() {
        basicTestDb(TWO_COL) {
            it.withConnection {
                it.withTransaction {
                    val statement = it.createStatement("INSERT INTO test(num, str)values(?,?)")
                    statement.bindLong(1, 123)
                    statement.bindString(2, "asdf")
                    statement.executeInsert()
                    statement.bindLong(1, 123)
                    statement.bindString(2, "asdf")
                    statement.executeInsert()
                    assertEquals(2, it.longForQuery("select count(*)from test"))
                    assertFails { it.beginTransaction() }
                    statement.finalizeStatement()
                }

                assertEquals(2, it.longForQuery("select count(*)from test"))
            }
        }
    }

    @Test
    fun rollbackLosesStatements() {
        basicTestDb(TWO_COL) {
            it.withConnection {
                assertFails {
                    it.withTransaction {
                        it.withStatement("INSERT INTO test(num, str)values(?,?)") {
                            bindLong(1, 123)
                            bindString(2, "asdf")
                            executeInsert()
                            bindLong(1, 123)
                            bindString(2, "asdf")
                            executeInsert()
                            assertEquals(2, it.longForQuery("select count(*)from test"))
                            executeInsert()
                        }
                    }
                }

                assertEquals(0, it.longForQuery("select count(*)from test"))
            }
        }
    }

    @Test
    fun sameStatementSqlDifferentInstances() {
        basicTestDb(TWO_COL) {
            it.withConnection {
                val s1 = it.createStatement("INSERT INTO test(num, str)values(?,?)")
                val s2 = it.createStatement("INSERT INTO test(num, str)values(?,?)")
                assertNotSame(s1, s2)
                assertNotEquals(
                    (s1 as NativeStatement).nativePointer,
                    (s2 as NativeStatement).nativePointer
                )

                s1.finalizeStatement()
                s2.finalizeStatement()
            }
        }
    }

    @Test
    fun badStatementFailsNoLeakedStatement() {
        basicTestDb(TWO_COL) {
            it.withConnection {
                assertFails { it.createStatement("slect * from test") }
            }
        }
    }

    @Test
    fun longForQuery() {
        basicTestDb(TWO_COL) {
            it.withConnection {
                it.withTransaction {
                    it.withStatement("INSERT INTO test(num, str)values(?,?)") {
                        bindLong(1, 123)
                        bindString(2, "asdf")
                        executeInsert()
                        bindLong(1, 123)
                        bindString(2, "asdf")
                        executeInsert()
                        assertEquals(2, it.longForQuery("select count(*)from test"))
                    }
                }
            }
        }
    }

    @Test
    fun longForQueryNotNumeric() {
        basicTestDb(TWO_COL) {
            it.withConnection {
                it.withTransaction {
                    it.withStatement("INSERT INTO test(num, str)values(?,?)") {
                        bindLong(1, 123)
                        bindString(2, "asdf")
                        executeInsert()
                        assertEquals(0, it.longForQuery("select str from test limit 1"))
                    }
                }
            }
        }
    }

    @Test
    fun stringForQuery() {
        basicTestDb(TWO_COL) {
            it.withConnection {
                it.withTransaction {
                    it.withStatement("INSERT INTO test(num, str)values(?,?)") {
                        bindLong(1, 123)
                        bindString(2, "asdf")
                        executeInsert()
                        assertEquals("asdf", it.stringForQuery("select str from test limit 1"))
                    }
                }
            }
        }
    }

    @Test
    fun stringForQueryNumeric() {
        basicTestDb(TWO_COL) {
            it.withConnection {
                it.withTransaction {
                    it.withStatement("INSERT INTO test(num, str)values(?,?)") {
                        bindLong(1, 123)
                        bindString(2, "asdf")
                        executeInsert()
                        bindLong(1, 123)
                        bindString(2, "asdf")
                        executeInsert()
                        assertEquals("2", it.stringForQuery("select count(*) from test"))
                    }
                }
            }
        }
    }

    @Test
    fun stringForQueryNullReturnsEmpty() {
        basicTestDb(FOUR_COL) {
            it.withConnection {
                it.withTransaction {
                    it.withStatement("INSERT INTO test(num, str, rrr)values(?,?,?)") {
                        bindLong(1, 123)
                        bindString(2, "asdf")
                        bindString(3, "qwert")
                        executeInsert()
                        assertEquals("", it.stringForQuery("select anotherStr from test limit 1"))
                    }
                }
            }
        }
    }

    @Test
    fun getVersion() {
        basicTestDb(TWO_COL) {
            it.withConnection {
                assertEquals(1, it.getVersion())
            }
        }
    }

    @Test
    fun setVersion() {
        basicTestDb(TWO_COL) {
            it.withConnection {
                it.setVersion(123)
                assertEquals(123, it.getVersion())
            }
        }
    }

    @Test
    fun journalMode() {
        basicTestDb(TWO_COL) {
            it.withConnection {
                assertEquals(it.journalMode, JournalMode.WAL)
            }
        }

        val secondDbName = "${TEST_DB_NAME}2"
        val man = createDatabaseManager(
            DatabaseConfiguration(
                name = secondDbName,
                version = 1,
                create = {},
                journalMode = JournalMode.DELETE
            )
        )

        man.withConnection {
            assertEquals(it.journalMode, JournalMode.DELETE)
        }

        deleteDatabase(secondDbName)
    }

    @Test
    fun memoryDatabase() {
        assertFalse(checkDbIsFile("chevychasevoicemail", true))
        assertTrue(checkDbIsFile("heyfile", false))
    }

    @Test
    fun memoryDatabaseMultipleConnections(){
        val memoryName = "asdfasdf"
        val man = createDatabaseManager(
            DatabaseConfiguration(
                name = memoryName,
                version = 1,
                create = {
                    it.withStatement(TWO_COL) {
                        execute()
                    }
                },
                inMemory = true
            )
        )

        val conn1 = man.createConnection()

        conn1.withTransaction {
                it.withStatement("insert into test(num, str)values(?,?)") {
                    bindLong(1, 232)
                    bindString(2, "asdf")
                    executeInsert()
                }
            }

        assertEquals(1, conn1.longForQuery("select count(*) from test"))

        val conn2 = man.createConnection()
        conn2.withTransaction {
            it.withStatement("insert into test(num, str)values(?,?)") {
                bindLong(1, 232)
                bindString(2, "asdf")
                executeInsert()
            }
        }

        assertEquals(2, conn2.longForQuery("select count(*) from test"))

        conn1.close()

        assertEquals(2, conn2.longForQuery("select count(*) from test"))

        conn2.close()

        man.withConnection {
            assertEquals(0, it.longForQuery("select count(*) from test"))
        }
    }

    private fun checkDbIsFile(memoryName: String, mem:Boolean): Boolean {
        var dbFileExists = false
        try {
            val man = createDatabaseManager(
                DatabaseConfiguration(
                    name = memoryName,
                    version = 1,
                    create = {
                        it.withStatement(TWO_COL) {
                            execute()
                        }
                    },
                    inMemory = mem
                )
            )

            man.withConnection {
                it.withTransaction {
                    it.withStatement("insert into test(num, str)values(?,?)") {
                        bindLong(1, 232)
                        bindString(2, "asdf")
                        executeInsert()
                    }
                }

                dbFileExists = NativeFileContext.databaseFile(memoryName).exists()
            }
        } finally {
            NativeFileContext.deleteDatabase(memoryName)
        }
        return dbFileExists
    }
}