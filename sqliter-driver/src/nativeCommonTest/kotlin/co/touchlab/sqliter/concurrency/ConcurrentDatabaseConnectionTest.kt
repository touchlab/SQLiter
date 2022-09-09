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

package co.touchlab.sqliter.concurrency

import co.touchlab.sqliter.*
import co.touchlab.sqliter.util.maybeFreeze
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ConcurrentDatabaseConnectionTest {

    @Test
    fun singleThreadedFaster() {
        println("single thread ${connTimer(true)}")
        println("multi thread ${connTimer(false)}")
    }

    fun connTimer(single: Boolean): Long {
        var totalTime = 0L
        basicTestDb(TWO_COL) {
            val INSERT_COUNT = 2000
            val INSERT_LOOP_COUNT = 100

            val conn = if (single) {
                it.createSingleThreadedConnection()
            } else {
                it.createMultiThreadedConnection()
            }
            val start = currentTimeMillis()

            val statement = conn.createStatement("insert into test(num, str)values(?,?)")

            try {
                for (outer in 0 until INSERT_LOOP_COUNT) {
                    conn.withTransaction {
                        for (i in 0 until INSERT_COUNT) {
                            statement.bindLong(1, 1223)
                            statement.bindString(2, "asdf")
                            statement.executeInsert()
                        }
                    }
                }
            } finally {
                statement.finalizeStatement()
            }

            totalTime = currentTimeMillis() - start

            assertEquals((INSERT_COUNT * INSERT_LOOP_COUNT).toLong(), conn.longForQuery("select count(*) from test"))
            conn.close()
        }

        return totalTime
    }


    @Test
    fun singleThreadedConnectionFreezeFails() {
        // Skip if not strict
        if (Platform.memoryModel != MemoryModel.STRICT) {
            return
        }
        basicTestDb(TWO_COL) {
            val conn = it.createSingleThreadedConnection()
            try {
                assertFails { conn.maybeFreeze() }
            } catch (assertion: AssertionError) {
                throw assertion
            } finally {
                conn.close()
            }
        }
    }

    @Test
    fun concurrentAccessWorks() {
        basicTestDb(TWO_COL) {
            val conn = it.createMultiThreadedConnection()
            //Run with this connection for seg fault
//            val conn = it.createSingleThreadedConnection()

            val ops = ThreadOps { Unit }

            val INSERT_COUNT = 200
            val INSERT_LOOP_COUNT = 100

            ops.exe {
                for (outer in 0 until INSERT_LOOP_COUNT) {
                    conn.withTransaction {
                        val statement = it.createStatement("insert into test(num, str)values(?,?)")
                        try {
                            for (i in 0 until INSERT_COUNT) {
                                statement.bindLong(1, 1223)
                                statement.bindString(2, "asdf")
                                statement.executeInsert()
                            }
                        } finally {
                            statement.finalizeStatement()
                        }
                    }
                }
            }

            val queryProc: (Unit) -> Unit = {
                var fullResult = false
                var tryCount = 0
                while (!fullResult) {
                    conn.withStatement("select * from test") {
                        val q = query()
                        var count = 0
                        var sum = 0L
                        while (q.next()) {
                            sum += q.getLong(0)
                            count++
                        }

                        fullResult = count == INSERT_COUNT * INSERT_LOOP_COUNT
                    }

                    if (tryCount++ > 100)
                        throw IllegalStateException("Guess we failed")
                }
            }

            ops.exe(queryProc)
            ops.exe(queryProc)

            ops.run(3)

            conn.close()
        }
    }
}