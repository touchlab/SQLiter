package co.touchlab.sqliter

import kotlin.system.getTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BasicTest{


    @Test
    fun pathTest(){
        val dbPathString = getDatabasePath("testdb").path
        println("DBPath: $dbPathString")
        assertTrue(dbPathString.endsWith("testdb"))
    }

    @Test
    fun createTable(){
        basicTestDb {manager ->
            val connection = manager.createConnection()

            val start = getTimeMillis()

            connection.withTransaction {
                val statement = it.createStatement("INSERT INTO test VALUES (?, ?, ?, ?)")
                for(i in 0 until 100_000) {
                    statement.bindLong(1, i.toLong())
                    statement.bindString(2, "Hilo $i")
                    statement.bindString(3, "asdf jfasdf $i fflkajsdf $i")
                    statement.bindString(4, "WWWWW QWER jfasdf $i fflkajsdf $i")
                    statement.executeInsert()
                    statement.resetStatement()
                }
                statement.finalizeStatement()
            }

            connection.withStatement("SELECT * FROM test") {
                val cursor = it.query()
                val timeBlocking = timeCursorBlocking(cursor) {
                    it.next()
                }

                println("Query timeBlocking: $timeBlocking")
            }

            println("Full run time ${getTimeMillis() - start}")
            connection.close()

        }
    }

    /*
    val opList = mutableListOf<()->Unit>()

        opList.add {
            connection.withStatement("SELECT * FROM test") {
                val cursor = it.query()
                val timeBlocking = timeCursorBlocking(cursor) {
                    it.nextBlocking()
                }

                println("Query timeBlocking: $timeBlocking")
            }
        }

        opList.add {
            connection.withStatement("SELECT * FROM test") {
                runBlocking {
                    val cursor = it.query()
                    val timeNonSuspend = timeCursor(cursor) {
                        it.nextNonSuspend()
                    }

                    println("Query timeNonSuspend: $timeNonSuspend")
                }
            }
        }

        opList.add {
            connection.withStatement("SELECT * FROM test") {
                runBlocking {
                    val cursor = it.query()
                    val timeSuspend = timeCursor(cursor) {
                        it.next()
                    }

                    println("Query timeSuspend: $timeSuspend")
                }
            }
        }

        opList.shuffle()

        opList.forEach { it() }
     */

    /*suspend fun timeCursor(cursor:Cursor,  proc:suspend (Cursor)->Boolean):Long{
        val start = getTimeMillis()
        var rowCount = 0
        while (proc(cursor)) {
            rowCount++
            assertTrue(cursor.getLong(0) > -1)
            assertTrue(cursor.getString(1).isNotEmpty())
        }

        val names = cursor.columnNames
        assertEquals(2, names.size)

        return getTimeMillis() - start
    }*/

    inline fun timeCursorBlocking(cursor:Cursor,  proc:(Cursor)->Boolean):Long{
        val start = getTimeMillis()
        var rowCount = 0
        while (proc(cursor)) {
            rowCount++
            assertTrue(cursor.getLong(0) > -1)
            assertTrue(cursor.getString(1).isNotEmpty())
            assertTrue(cursor.getString(2).isNotEmpty())
            assertTrue(cursor.getString(3).isNotEmpty())
        }

        val names = cursor.columnNames
        assertEquals(4, names.size)

        return getTimeMillis() - start
    }
}