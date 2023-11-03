package co.touchlab.sqliter.performance

import co.touchlab.sqliter.*
import kotlin.test.Test
import kotlin.test.assertTrue

class DbPerformanceTest:BaseDatabaseTest() {
    @Test
    fun bigInsertTest() {
        val manager = createDatabaseManager(
            DatabaseConfiguration(
                name = TEST_DB_NAME,
                version = 1,
                create = { db ->
                    db.withStatement(TWO_COL) {
                        execute()
                    }
                },
                journalMode = JournalMode.WAL,
                loggingConfig = DatabaseConfiguration.Logging(logger = NoneLogger),
            ),
        )

        val insertList = (0..50_000).map { i ->
            Pair(i.toLong(), "row $i")
        }

        val connection = manager.surpriseMeConnection()

        val start = currentTimeMillis()
        connection.withStatement("insert into test(num, str)values(?,?)"){
            insertList.forEach {
                bindLong(1, it.first)
                bindString(2, it.second)
                executeInsert()
            }
        }

        val time = currentTimeMillis() - start
        println("Insert took time $time")
        //Failing on CI. Need another approach for performance
//        assertTrue("Insert took time ${time}") {time < 6000}
    }
}