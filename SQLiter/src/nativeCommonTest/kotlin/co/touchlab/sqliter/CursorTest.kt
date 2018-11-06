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
                    it.execute()
                }
            }))

        val connection = manager.createConnection()
        connection.withStatement("insert into test(num, str)values(?,?)"){
            it.bindLong(1, 2)
            it.bindString(2, "asdf")
            it.executeInsert()
            it.bindLong(1, 3)
            it.bindString(2, "qwert")
            it.executeInsert()
        }

        connection.withStatement("select * from test"){
            var rowCount = 0
            it.query().iterator().forEach {
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