package co.touchlab.sqliter.concurrency

import co.touchlab.sqliter.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ConcurrentPlaygroundTest {
    /**
     * Playing with how connections and transactions interact
     */
    @Test
    fun testLongRead() {
        basicTestDb(TWO_COL, timeout = 1000) { dbman ->
            val conn = dbman.createMultiThreadedConnection()
            val conn2 = dbman.createMultiThreadedConnection()

            insert10(conn)

            val queryStatement = conn.createStatement("select * from test")
            val cursor = queryStatement.query()
            cursor.next()

            try {
                assertEquals(10, countRows(conn2))
                insert10(conn2)
                assertEquals(10, countRows(conn))
                queryStatement.finalizeStatement()
                assertEquals(20, countRows(conn))
            } finally {

                conn.close()
                conn2.close()
            }
        }
    }

    private fun countRows(conn: DatabaseConnection): Long = conn.withStatement("select count(*) from test"){
        val c = query()
        c.next()
        c.getLong(0)
    }

    private fun insert10(conn: DatabaseConnection) {
        conn.withTransaction {
            repeat(10) { i ->
                it.withStatement("insert into test(num, str)values(?,?)") {
                    bindLong(1, i.toLong())
                    bindString(2, "arst $i")
                    executeInsert()
                }
            }
        }
    }

}