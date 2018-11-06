package co.touchlab.sqliter

import kotlin.test.*

class DatabaseConfigurationTest{
    @BeforeEach
    fun before(){
        deleteDatabase("testdb")
    }

    @AfterEach
    fun after(){
        deleteDatabase("testdb")
    }

    @Test
    fun journalModeSetting()
    {
        val manager = createDatabaseManager(DatabaseConfiguration(name = "testdb", version = 1,
            journalMode = JournalMode.WAL, create = { db ->
                db.withStatement(TWO_COL) {
                    it.execute()
                }
            }))

        val conn = manager.createConnection()
        println("tr 0")
        assertEquals(conn.journalMode, JournalMode.WAL)
        println("tr 1")
        println("Update journal to DELETE result ${conn.updateJournalMode(JournalMode.DELETE)}")
        assertEquals(conn.journalMode, JournalMode.DELETE)
        println("Update journal to WAL result ${conn.updateJournalMode(JournalMode.WAL)}")

        conn.withStatement("insert into test(num, str)values(?,?)"){
            it.bindLong(1, 333)
            it.bindString(2, "asdf")
            it.executeInsert()
        }

        println("Update journal to DELETE result ${conn.updateJournalMode(JournalMode.DELETE)}")
        assertEquals(conn.journalMode, JournalMode.DELETE)

        conn.close()

        println("tr 2")
        val manager2 = createDatabaseManager(DatabaseConfiguration(name = "testdb", version = 1,
            journalMode = JournalMode.WAL, create = {
                fail("Same version shouldn't run")
            }))

        val conn2 = manager2.createConnection()
        assertEquals(conn2.journalMode, JournalMode.WAL)
        conn2.close()
    }
}