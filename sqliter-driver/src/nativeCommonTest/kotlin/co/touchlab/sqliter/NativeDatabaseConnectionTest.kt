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

import co.touchlab.sqliter.util.maybeFreeze
import platform.posix.usleep
import kotlin.concurrent.AtomicInt
import kotlin.native.concurrent.Future
import kotlin.native.concurrent.FutureState
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.system.getTimeMillis
import kotlin.test.*

class NativeDatabaseConnectionTest : BaseDatabaseTest(){

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
                name = TEST_DB_NAME, version = 1,
                journalMode = mode,
                create = { db ->
                    db.withStatement(TWO_COL) {
                        execute()
                    }
                },
                extendedConfig = DatabaseConfiguration.Extended(busyTimeout = 30000),
                loggingConfig = DatabaseConfiguration.Logging(logger = NoneLogger),
            )
        )

        val mainConn = manager.surpriseMeConnection()

        val biginsert = { conn: DatabaseConnection ->
            conn.withTransaction {
                it.withStatement("insert into test(num, str)values(?,?)") {
                    for (i in 0 until 50_000) {
                        bindLong(1, i.toLong())
                        bindString(2, "row $i")
                        executeInsert()
                    }
                }
            }
        }

        val bigselect = { conn: DatabaseConnection ->
            val rows = conn.longForQuery("select count(*) from test")
            conn.withStatement("select * from test limit 1000 offset ${rows - 2000}") {
                var rowCount = 0
                query().iterator().forEach { rowCount++ }
                assertEquals(rowCount, 1000)
            }
        }

        //Seed data
        biginsert(mainConn)

        val workers = Array(10) { Worker.start() }
        val futures = mutableListOf<Future<Unit>>()
        workers.forEach {
            futures.add(it.execute(TransferMode.SAFE, { ManagerOps(manager, biginsert, bigselect).maybeFreeze() }) {
                val conn = it.manager.surpriseMeConnection()
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
                name = TEST_DB_NAME, version = 1,
                journalMode = JournalMode.WAL,
                create = { db ->
                    db.withStatement(TWO_COL) {
                        execute()

                    }
                    db.withStatement("insert into test(num, str)values(?,?)") {
                        bindLong(1, 555)
                        bindString(2, "qwerqwer")
                        executeInsert()
                        resetStatement()
                        bindLong(1, 545)
                        bindString(2, "qasdfwerqwer")
                        executeInsert()

                    }
                },
                extendedConfig = DatabaseConfiguration.Extended(busyTimeout = 3000),
                loggingConfig = DatabaseConfiguration.Logging(logger = NoneLogger),
            )
        )

        val reader = manager.surpriseMeConnection()
        val writer = manager.surpriseMeConnection()

        assertEquals(reader.journalMode, JournalMode.WAL)

        reader.withStatement("select num, str from test limit 20") {
            val cursor = query()
            cursor.next()
            val start = getTimeMillis()

            writer.withStatement("insert into test(num, str)values(?,?)") {
                bindLong(1, 1)
                bindString(2, "a")
                executeInsert()
                resetStatement()
                bindLong(1, 2)
                bindString(2, "b")
                executeInsert()
            }

            val time = getTimeMillis() - start
            //Verify probably worked without delay
            assertTrue(time < 100)

            println("write timeout $time")

            cursor.statement.resetStatement()
        }

        assertEquals(4, reader.longForQuery("select count(*) from test"))
        reader.close()
        writer.close()
    }

    @Test
    fun failTimeoutWriteWhileCursorOpenJournal() {

        val manager = createDatabaseManager(
            DatabaseConfiguration(
                name = TEST_DB_NAME, version = 1,
                create = { db ->
                    db.withStatement(TWO_COL) {
                        execute()

                    }
                    db.withStatement("insert into test(num, str)values(?,?)") {
                        bindLong(1, 555)
                        bindString(2, "qwerqwer")
                        executeInsert()
                        resetStatement()
                        bindLong(1, 545)
                        bindString(2, "qasdfwerqwer")
                        executeInsert()
                    }
                },
                journalMode = JournalMode.DELETE,
                extendedConfig = DatabaseConfiguration.Extended(busyTimeout = 1500),
                loggingConfig = DatabaseConfiguration.Logging(logger = NoneLogger),
            )
        )

        val reader = manager.surpriseMeConnection()
        val writer = manager.surpriseMeConnection()

        try {
            assertEquals(reader.journalMode, JournalMode.DELETE)

            reader.withStatement("select num, str from test limit 20") {
                val cursor = query()
                cursor.next()
                val start = getTimeMillis()

                assertFails {
                    writer.withStatement("insert into test(num, str)values(?,?)") {
                        bindLong(1, 1)
                        bindString(2, "a")
                        executeInsert()
                        resetStatement()
                        bindLong(1, 2)
                        bindString(2, "b")
                        executeInsert()
                    }
                }

                val time = getTimeMillis() - start
                //Verify timeout on writer
                assertTrue(time > 1300)

                println("write timeout $time")

                cursor.statement.resetStatement()
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
                name = TEST_DB_NAME,
                version = 1,
                create = { db ->
                    db.withStatement(TWO_COL) {
                        execute()
                    }
                    db.withStatement("insert into test(num, str)values(?,?)") {
                        bindLong(1, 555)
                        bindString(2, "qwerqwer")
                        executeInsert()
                        resetStatement()
                        bindLong(1, 545)
                        bindString(2, "qasdfwerqwer")
                        executeInsert()
                    }
                },
                journalMode = JournalMode.DELETE,
                extendedConfig = DatabaseConfiguration.Extended(busyTimeout = 3000),
                loggingConfig = DatabaseConfiguration.Logging(logger = NoneLogger),
            )
        )

        threadWait(3000, manager) {
            val queryBlock: (DatabaseConnection) -> Unit = {
                it.withStatement("select num, str from test limit 20") {
                    val start = getTimeMillis()
                    val cursor = query()
                    cursor.next()
                    try {
                        usleep(4_000_000u)
                        while (cursor.next()) {
                            println("cursor ${cursor.getLong(0)}/${cursor.getString(1)}")
                        }

                        println("Run time ${getTimeMillis() - start}")
                    } finally {
                        cursor.statement.resetStatement()
                    }
                }

            }

            println("FIRST RUN")
            queryBlock(it)
            usleep(4_000_000u)
            println("SECOND RUN")
            queryBlock(it)
        }
    }

//    @Test
    fun testTimeout() {
        val manager = createDatabaseManager(DatabaseConfiguration(
            name = TEST_DB_NAME,
            version = 1,
            create = { db ->
                db.withStatement(TWO_COL) {
                    execute()
                }
            },
            extendedConfig = DatabaseConfiguration.Extended(busyTimeout = 4000),
            loggingConfig = DatabaseConfiguration.Logging(logger = NoneLogger)))

        val block: (DatabaseConnection) -> Unit = {
            it.withTransaction {
                it.withStatement("insert into test(num, str)values(?,?)") {
                    for (i in 0 until 10_000) {
                        bindLong(1, i.toLong())
                        bindString(2, "Oh $i")
                        executeInsert()
                    }
                }
            }
        }
        assertTrue(threadWait(1000, manager, block))
        assertFalse(threadWait(5000, manager, block))
    }

    /*@Test
    fun testClosedThrows(){
        val man = createDb()
        val conn = man.surpriseMeConnection()
        val goInsert: Statement.() -> Long = {
            bindLong(1, 123)
            bindString(2, "hello")
            executeInsert()
        }
        val insertSql = "insert into test(num, str)values(?,?)"
        conn.withStatement(insertSql, goInsert)
        conn.close()
        assertFails { conn.withStatement(insertSql, goInsert) }
    }*/

    @Test
    fun testFailedCloseRecall(){
        val manager = basicDb()

        val conn = manager.createMultiThreadedConnection()
        val stmt = conn.createStatement("select * from test")

        //FYI: This will log an error, but that's OK. We're doing the logging ourselves.
        //ERROR - sqlite3_close(0x[whatever]) failed: 5
        conn.close()
        stmt.finalizeStatement()
        assertFails { conn.close() }
    }

    @Test
    fun lateQueryCloseSucceeds(){
        val manager = basicDb()

        val conn = manager.createMultiThreadedConnection()
        val stmt = conn.createStatement("select * from test")
        stmt.query()

        //After connection close, we should still be able to finalize statements
        conn.close()
        stmt.finalizeStatement()
    }

    private fun basicDb() = createDatabaseManager(
        DatabaseConfiguration(
            name = TEST_DB_NAME, version = 1,
            create = { db ->
                db.withStatement(TWO_COL) {
                    execute()
                }
            },
            extendedConfig = DatabaseConfiguration.Extended(busyTimeout = 15000),
            loggingConfig = DatabaseConfiguration.Logging(logger = NoneLogger),
        )
    )

    //    @Test
    fun multipleConnectionsAndVersion() {

        val upgradeCalled = AtomicInt(0)
        val config1 = DatabaseConfiguration(
            name = TEST_DB_NAME,
            version = 1,
            journalMode = JournalMode.WAL,
            create = { db ->
                db.withStatement(TWO_COL) {
                    execute()
                }
            },
            upgrade = {_,_,_ ->
                throw IllegalStateException("This shouldn't happen")
            },
            extendedConfig = DatabaseConfiguration.Extended(busyTimeout = 3000),
            loggingConfig = DatabaseConfiguration.Logging(logger = NoneLogger),
        )

        val manager = createDatabaseManager(config1)
        manager.createMultiThreadedConnection().close()

        val config2 = config1.copy(
            version = 2,
            create = {
                throw IllegalStateException("This shouldn't happen")
            },
            upgrade = {_,_,_ ->
                if(!upgradeCalled.compareAndSet(0, 1))
                    throw IllegalStateException("Multiple upgrade calls")
            }
        )

        val workers = (0 until 20).map { Worker.start(errorReporting = true, name = "Test Worker $it") }
        val futures = workers.map { worker ->
            worker.execute(TransferMode.SAFE, { config2.maybeFreeze() }) {
                val managerInner = createDatabaseManager(it)
                val conn = managerInner.createMultiThreadedConnection()
                conn.close()
            }
        }

        futures.forEach {
            it.result
            if(it.state == FutureState.THROWN)
                throw IllegalStateException("db failed")
        }
    }

    private fun threadWait(time: Int, manager: DatabaseManager, block: (DatabaseConnection) -> Unit): Boolean {
        return manager.withConnection {
            val worker = Worker.start()
            val future = worker.execute(TransferMode.SAFE, { Pair(manager, block).maybeFreeze() }) {
                try {
                    usleep(500_000u)
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
                        bindLong(1, i.toLong())
                        bindString(2, "Hey $i")
                        executeInsert()
                    }
                    usleep((time * 1000).toUInt())
                    for (i in 0 until 10_000) {
                        bindLong(1, i.toLong())
                        bindString(2, "Hey $i")
                        executeInsert()
                    }
                }
            }

            val result = future.result
            worker.requestTermination()
            result
        }
    }

    private fun createDb() =
        createDatabaseManager(
            DatabaseConfiguration(
                name = TEST_DB_NAME,
                version = 1,
                create = { db ->
                    db.withStatement(TWO_COL) {
                        execute()
                    }
                },
                journalMode = JournalMode.WAL,
                extendedConfig = DatabaseConfiguration.Extended(busyTimeout = 30000),
                loggingConfig = DatabaseConfiguration.Logging(logger = NoneLogger),
            )
        )
}
