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