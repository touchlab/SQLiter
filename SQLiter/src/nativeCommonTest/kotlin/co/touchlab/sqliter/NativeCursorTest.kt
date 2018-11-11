package co.touchlab.sqliter

import kotlin.test.*

class NativeCursorTest {

    @BeforeEach
    fun before() {
        deleteDatabase("testdb")
    }

    @AfterEach
    fun after() {
        deleteDatabase("testdb")
    }

    @Test
    fun beforeNextFails() {
        withSample2Col { conn ->
            conn.withStatement("select * from test") {
                val cursor = it.query()
                assertFails {
                    cursor.getLong(0)
                }
            }
        }
    }

    @Test
    fun afterDoneFails() {
        withSample2Col { conn ->
            conn.withStatement("select * from test") {
                val cursor = it.query()
                while (cursor.next()){
                    //Meh
                }
                assertFails { cursor.next() }
            }
        }
    }

    @Test
    fun colNames(){
        withSample2Col { conn ->
            conn.withStatement("select * from test") {
                val cursor = it.query()

                assertEquals(2, cursor.columnCount)
            }
        }
    }

    private fun withSample2Col(block: (DatabaseConnection) -> Unit) {
        val manager = createDb()
        manager.withConnection { conn ->
            conn.withStatement("insert into test(num, str)values(?,?)") {
                it.bindLong(1, 22)
                it.bindString(2, "asdf")
                it.executeInsert()
                it.bindLong(1, 33)
                it.bindString(2, "qwert")
                it.executeInsert()
                1
            }

            block(conn)
        }
    }

    private fun createDb() =
        createDatabaseManager(
            DatabaseConfiguration(
                name = "testdb", version = 1,
                journalMode = JournalMode.WAL, create = { db ->
                    db.withStatement(TWO_COL) {
                        it.execute()

                    }

                }, busyTimeout = 30000
            )
        )

}