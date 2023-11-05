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

import co.touchlab.sqliter.native.increment
import kotlin.concurrent.AtomicInt
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze
import kotlin.test.Test
import kotlin.test.assertEquals

class MultithreadedTest {
    /*@Test
    fun multipleThreadsHammer(){
        basicTestDb {
            val totalRun = AtomicInt(0)
            val conn = it.createMultiThreadedConnection()
            val ops = ThreadOps{Unit}
            for(i in 0 until 200) {
                opInsert(ops, conn, totalRun)
                opInsert(ops, conn, totalRun)
                opInsert(ops, conn, totalRun)
                opInsert(ops, conn, totalRun)
                opInsert(ops, conn, totalRun)
                opQuery(ops, conn)
            }

            timer {
                ops.run(10)
            }

            assertEquals(1_000_000L, conn.longForQuery("select count(*) from test"))
        }
    }
*/
    inline fun timer(block:()->Unit){
        val start = currentTimeMillis()
        try {
            block()
        }finally {
            println("Total time ${currentTimeMillis() - start}")
        }
    }

    private fun opQuery(ops: ThreadOps<Unit>, conn: DatabaseConnection) {
        ops.exe {
            conn.withStatement("select * from test limit 20000") {
                val result = query()
                var rowCount = 0
                while (result.next()) {
                    rowCount++
                }
            }
        }
    }

    private fun opInsert(
        ops: ThreadOps<Unit>,
        conn: DatabaseConnection,
        totalRun: AtomicInt
    ) {
        ops.exe {
            conn.withTransaction {
                conn.withStatement("insert into test(num, str, rrr)values(?, ?, ?)") {
                    for (i in 0 until 1000) {
                        bindLong(1, i.toLong())
                        bindString(2, "str $i")
                        bindString(3, "rrr $i")
                    }
                }
            }
            totalRun.increment()
        }
    }
    /*@Test
    fun journalMultipleInsert() {
        basicTestDb(TWO_COL) { manager ->
            val connection = manager.createConnection()
//            connection.journalMode = JournalMode.WAL
            val worker = Worker.start()
            val future = worker.execute(TransferMode.SAFE, { manager.freeze() }) {
                val connection = it.createConnection()
                connection.withTransaction {
                    it.withStatement("insert into test(num, str)values(?,?)") {
                        usleep(500_000)
                        for (i in 0 until 10_000) {
                            bindLong(1, i.toLong())
                            bindString(2, "Oh $i")
                            executeInsert()
                        }
                    }
                }
            }
            connection.withTransaction {
                it.withStatement("insert into test(num, str)values(?,?)") {
                    for (i in 0 until 10_000) {
                        bindLong(1, i.toLong())
                        bindString(2, "Hey $i")
                        executeInsert()
                    }
                    usleep(9_000_000)
                    for (i in 0 until 10_000) {
                        bindLong(1, i.toLong())
                        bindString(2, "Hey $i")
                        executeInsert()
                    }
                }
            }
            future.consume { println("We got here") }
            connection.withStatement("select count(*) from test") {
                assertEquals(30_000, query().forLong())
            }
        }
    }*/
}
