package co.touchlab.sqlager.user

import co.touchlab.sqliter.DatabaseConfiguration
import kotlin.test.*

class DatabaseInstanceTest {
    lateinit var database: Database

    @BeforeTest
    fun setup() {
        val man = createDatabaseManager(DatabaseConfiguration(
            name = "testdb",
            version = 1,
            inMemory = true,
            create = {
                val instance = wrapDatabaseInstance(it)
                instance.execute(TWO_COL)
            }
        ))

        database = Database(man, 20)
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun cacheClears() {
        for (i in 0 until 30) {
            database.insert("insert into test(num, str)values($i,'$i')")
        }

        assertEquals(20, database.localInstance { statementCache.size })
        assertTrue(database.close())
    }

    @Test
    fun badStatementsFail() {
        database.instance { instance ->
            assertFails { instance.execute("Mac and me") }
            assertFails { instance.insert("Mac and me") }
            assertFails { instance.updateDelete("Mac and me") }
            assertFails {
                instance.query("Mac and me") {
                }
            }
        }
    }


}