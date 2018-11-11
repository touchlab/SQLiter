package co.touchlab.sqliter

import platform.posix.usleep
import kotlin.native.concurrent.*
import kotlin.system.getTimeMillis
import kotlin.test.*

class NativeDatabaseConnectionTest {
    @BeforeEach
    fun before() {
        deleteDatabase("testdb")
    }

    @AfterEach
    fun after() {
        deleteDatabase("testdb")
    }

//    @Test
    fun multithreadedActivityWAL() {
        multithreadedActivity(JournalMode.WAL)
    }

//    @Test
    fun multithreadedActivityDELETE() {
        multithreadedActivity(JournalMode.DELETE)
    }

    fun multithreadedActivity(mode: JournalMode) {
        val start = getTimeMillis()
        val manager = createDatabaseManager(
            DatabaseConfiguration(
                name = "testdb", version = 1,
                journalMode = mode, create = { db ->
                    db.withStatement(TWO_COL) {
                        it.execute()

                    }

                }, busyTimeout = 30000
            )
        )

        val mainConn = manager.createConnection()

        val biginsert = { conn: DatabaseConnection ->
            conn.withTransaction {
                it.withStatement("insert into test(num, str)values(?,?)") {
                    for (i in 0 until 50_000) {
                        it.bindLong(1, i.toLong())
                        it.bindString(2, "row $i")
                        it.executeInsert()
                    }
                }
            }
        }

        val bigselect = { conn: DatabaseConnection ->
            val rows = conn.longForQuery("select count(*) from test")
            conn.withStatement("select * from test limit 1000 offset ${rows - 2000}") {
                var rowCount = 0
                it.query().iterator().forEach { rowCount++ }
                assertEquals(rowCount, 1000)
            }
        }

        //Seed data
        biginsert(mainConn)

        val workers = Array(10) { Worker.start() }
        val futures = mutableListOf<Future<Unit>>()
        workers.forEach {
            futures.add(it.execute(TransferMode.SAFE, { ManagerOps(manager, biginsert, bigselect).freeze() }) {
                val conn = it.manager.createConnection()
                try {
                    for (i in 0 until 10) {
                        if (i % 2 == 0) {
                            it.biginsert(conn)
                        } else {
                            it.bigselect(conn)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    conn.close()
                }

            }
            )
        }

        futures.waitForAllFutures()
        workers.forEach { it.requestTermination() }

        assertEquals(2_550_000, mainConn.longForQuery("select count(*) from test"))
        println("time ${mode.name} ${getTimeMillis() - start}")
        mainConn.close()
    }

    data class ManagerOps(
        val manager: DatabaseManager,
        val biginsert: (DatabaseConnection) -> Unit,
        val bigselect: (DatabaseConnection) -> Unit
    )

    @Test
    fun canWriteWhileCursorOpenWAL() {

        val manager = createDatabaseManager(
            DatabaseConfiguration(
                name = "testdb", version = 1,
                journalMode = JournalMode.WAL, create = { db ->
                    db.withStatement(TWO_COL) {
                        it.execute()

                    }
                    db.withStatement("insert into test(num, str)values(?,?)") {
                        it.bindLong(1, 555)
                        it.bindString(2, "qwerqwer")
                        it.executeInsert()
                        it.reset()
                        it.bindLong(1, 545)
                        it.bindString(2, "qasdfwerqwer")
                        it.executeInsert()

                    }
                }, busyTimeout = 3000
            )
        )

        val reader = manager.createConnection()
        val writer = manager.createConnection()

        assertEquals(reader.journalMode, JournalMode.WAL)

        reader.withStatement("select num, str from test limit 20") {
            val cursor = it.query()
            cursor.next()
            val start = getTimeMillis()

            writer.withStatement("insert into test(num, str)values(?,?)") {
                it.bindLong(1, 1)
                it.bindString(2, "a")
                it.executeInsert()
                it.reset()
                it.bindLong(1, 2)
                it.bindString(2, "b")
                it.executeInsert()
            }

            val time = getTimeMillis() - start
            //Verify probably worked without delay
            assertTrue(time < 100)

            println("write timeout $time")

            cursor.close()
        }

        assertEquals(4, reader.longForQuery("select count(*) from test"))
        reader.close()
        writer.close()
    }

    @Test
    fun failTimeoutWriteWhileCursorOpenJournal() {

        val manager = createDatabaseManager(
            DatabaseConfiguration(
                name = "testdb", version = 1,
                journalMode = JournalMode.DELETE, create = { db ->
                    db.withStatement(TWO_COL) {
                        it.execute()

                    }
                    db.withStatement("insert into test(num, str)values(?,?)") {
                        it.bindLong(1, 555)
                        it.bindString(2, "qwerqwer")
                        it.executeInsert()
                        it.reset()
                        it.bindLong(1, 545)
                        it.bindString(2, "qasdfwerqwer")
                        it.executeInsert()

                    }
                }, busyTimeout = 1500
            )
        )

        val reader = manager.createConnection()
        val writer = manager.createConnection()

        try {
            assertEquals(reader.journalMode, JournalMode.DELETE)

            reader.withStatement("select num, str from test limit 20") {
                val cursor = it.query()
                cursor.next()
                val start = getTimeMillis()

                assertFails {
                    writer.withStatement("insert into test(num, str)values(?,?)") {
                        it.bindLong(1, 1)
                        it.bindString(2, "a")
                        it.executeInsert()
                        it.reset()
                        it.bindLong(1, 2)
                        it.bindString(2, "b")
                        it.executeInsert()
                    }
                }

                val time = getTimeMillis() - start
                //Verify timeout on writer
                assertTrue(time > 1300)

                println("write timeout $time")

                cursor.close()
            }
        } finally {
            reader.close()
            writer.close()
        }
    }

    @Test
    fun testReadWhileWriting() {
        val manager = createDatabaseManager(
            DatabaseConfiguration(
                name = "testdb", version = 1,
                journalMode = JournalMode.DELETE, create = { db ->
                    db.withStatement(TWO_COL) {
                        it.execute()

                    }
                    db.withStatement("insert into test(num, str)values(?,?)") {
                        it.bindLong(1, 555)
                        it.bindString(2, "qwerqwer")
                        it.executeInsert()
                        it.reset()
                        it.bindLong(1, 545)
                        it.bindString(2, "qasdfwerqwer")
                        it.executeInsert()

                    }
                }, busyTimeout = 3000
            )
        )

        threadWait(3000, manager) {
            val queryBlock: (DatabaseConnection) -> Unit = {
                it.withStatement("select num, str from test limit 20") {
                    val start = getTimeMillis()
                    val cursor = it.query()
                    cursor.next()
                    try {
                        usleep(4_000_000)
                        while (cursor.next()) {
                            println("cursor ${cursor.getLong(0)}/${cursor.getString(1)}")
                        }

                        println("Run time ${getTimeMillis() - start}")
                    } finally {
                        cursor.close()
                    }
                }

            }

            println("FIRST RUN")
            queryBlock(it)
            usleep(4_000_000)
            println("SECOND RUN")
            queryBlock(it)
        }
    }

    @Test
    fun testTimeout() {
        val manager = createDatabaseManager(DatabaseConfiguration(name = "testdb", version = 1, create = { db ->
            db.withStatement(TWO_COL) {
                it.execute()
            }
        }, busyTimeout = 3000))

        val block: (DatabaseConnection) -> Unit = {
            it.withTransaction {
                it.withStatement("insert into test(num, str)values(?,?)") {


                    for (i in 0 until 10_000) {
                        it.bindLong(1, i.toLong())
                        it.bindString(2, "Oh $i")
                        it.executeInsert()
                    }
                }
            }
        }
        assertTrue(threadWait(1500, manager, block))
        assertFalse(threadWait(5000, manager, block))
    }

    private fun threadWait(time: Int, manager: DatabaseManager, block: (DatabaseConnection) -> Unit): Boolean {
        return manager.withConnection {
            val worker = Worker.start()
            val future = worker.execute(TransferMode.SAFE, { Pair(manager, block).freeze() }) {
                try {
                    usleep(500_000)
                    it.first.withConnection(it.second)
                    return@execute true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@execute false
                }
            }

            it.withTransaction {
                it.withStatement("insert into test(num, str)values(?,?)") {
                    for (i in 0 until 10_000) {
                        it.bindLong(1, i.toLong())
                        it.bindString(2, "Hey $i")
                        it.executeInsert()
                    }
                    usleep((time * 1000).toUInt())
                    for (i in 0 until 10_000) {
                        it.bindLong(1, i.toLong())
                        it.bindString(2, "Hey $i")
                        it.executeInsert()
                    }
                }
            }

            val result = future.result
            worker.requestTermination()
            result
        }
    }
}