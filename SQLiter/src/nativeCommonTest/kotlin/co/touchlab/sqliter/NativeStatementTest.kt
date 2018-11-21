package co.touchlab.sqliter

import kotlin.test.*

class NativeStatementTest {

    @BeforeEach
    fun before() {
        deleteDatabase("testdb")
    }

    @AfterEach
    fun after() {
        deleteDatabase("testdb")
    }

    @Test
    fun insertStatement() {
        basicTestDb {
            val connection = it.createConnection()
            val statement = connection.createStatement("INSERT INTO test VALUES (?, ?, ?, ?)")
            for (i in 0 until 50) {
                statement.bindLong(1, i.toLong())
                statement.bindString(2, "Hilo $i")
                if (i % 2 == 0)
                    statement.bindString(3, null)
                else
                    statement.bindString(3, "asdf jfasdf $i fflkajsdf $i")
                statement.bindString(4, "WWWWW QWER jfasdf $i fflkajsdf $i")
                statement.executeInsert()
            }
            statement.finalizeStatement()

            connection.withStatement("select str from test") {
                val query = query()
                query.next()
                assertTrue(query.getString(query.columnNames["str"]!!).startsWith("Hilo"))
            }
            connection.close()
        }
    }

    @Test
    fun updateStatement() {
        basicTestDb {
            val connection = it.createConnection()
            val statement = connection.createStatement("INSERT INTO test VALUES (?, ?, ?, ?)")
            for (i in 0 until 50) {
                statement.bindLong(1, i.toLong())
                statement.bindString(2, "Hilo $i")
                if (i % 2 == 0)
                    statement.bindString(3, null)
                else
                    statement.bindString(3, "asdf jfasdf $i fflkajsdf $i")
                statement.bindString(4, "WWWWW QWER jfasdf $i fflkajsdf $i")
                statement.executeInsert()
            }
            statement.finalizeStatement()

            connection.withStatement("update test set str = ?") {
                bindString(1, "asdf")
                executeUpdateDelete()
            }

            connection.withStatement("select str from test") {
                val query = query()
                query.next()
                assertFalse(query.getString(query.columnNames["str"]!!).startsWith("Hilo"))
                assertTrue(query.getString(query.columnNames["str"]!!).startsWith("asdf"))
            }
            connection.close()
        }
    }


    @Test
    fun updateCountResult() {
        basicTestDb(TWO_COL) {
            val connection = it.createConnection()
            connection.withTransaction {
                it.withStatement("insert into test(num, str)values(?,?)") {
                    bindLong(1, 1)
                    bindString(2, "asdf")
                    assertTrue(executeInsert() > 0)
                    resetStatement()
                    bindLong(1, 2)
                    bindString(2, "rrr")
                    executeInsert()
                }
            }

            connection.withTransaction {
                it.withStatement("update test set str = ?") {
                    bindString(1, "qwert")
                    assertEquals(2, executeUpdateDelete())
                }
            }

            connection.close()
        }
    }

    @Test
    fun statementIndexIssues() {
        basicTestDb(TWO_COL) {
            it.withConnection {
                it.withStatement("insert into test(num, str)values(?,?)") {
                    assertFails { bindString(0, "asdf") }
                    assertFails { bindString(3, "asdf") }

                    //Still works?
                    bindLong(1, 123)
                    bindString(2, "asdf")
                    assertTrue(executeInsert() > 0)
                }
            }
        }
    }

    @Test
    fun failNotNullNull() {
        basicTestDb(TWO_COL) {
            it.withConnection {
                it.withStatement("insert into test(num, str)values(?,?)") {
                    bindLong(1, 333)
                    bindString(2, null)
                    assertFails { executeInsert() }
                }
            }
        }
    }

    @Test
    fun failBadFormat() {
        basicTestDb(TWO_COL) {
            it.withConnection {
                assertFails { it.createStatement("insert into test(num, str)values(?,?") }
            }
        }
    }

    @Test
    fun failPartialBind() {
        basicTestDb(TWO_COL) {
            it.withConnection {
                it.withStatement("insert into test(num, str)values(?,?)") {
                    bindLong(1, 21)
                    bindString(2, "asdf")
                    executeInsert()
                    bindLong(1, 44)
                    assertFails { executeInsert() }
                }
            }
        }
    }

    @Test
    fun testClosedThrows(){
        basicTestDb(TWO_COL) {
            it.withConnection {
                val statement = it.createStatement("insert into test(num, str)values(?,?)")
                statement.bindLong(1, 21)
                statement.bindString(2, "asdf")
                statement.executeInsert()
                statement.finalizeStatement()
                assertFails {
                    statement.bindLong(1, 22)
                }
                it.withStatement("insert into test(num, str)values(?,?)") {
                    bindLong(1, 21)
                    bindString(2, "asdf")
                    executeInsert()
                    bindLong(1, 44)
                    assertFails { executeInsert() }
                }
            }
        }
    }

    @Test
    fun paramByName() {
        basicTestDb(TWO_COL) {
            it.withConnection {
                it.withStatement("insert into test(num, str)values(@num,@str)") {
                    val index = bindParameterIndex("@num")
                    assertEquals(1, index)
                    bindLong(index, 21)
                    bindString(bindParameterIndex("@str"), "asdf")
                    executeInsert()
                }
                it.withStatement("insert into test(num, str)values(:num,\$str)") {
                    bindLong(bindParameterIndex(":num"), 21)
                    bindString(bindParameterIndex("\$str"), "asdf")
                    executeInsert()
                }

                assertEquals(2, it.longForQuery("select count(*) from test where num = 21"))
            }
        }
    }

    @Test
    fun paramByNameNotFound() {
        basicTestDb(TWO_COL) {
            it.withConnection {
                it.withStatement("insert into test(num, str)values(@num,@str)") {
                    assertFails {
                        bindParameterIndex("@numm")
                    }
                }
            }
        }
    }
}

