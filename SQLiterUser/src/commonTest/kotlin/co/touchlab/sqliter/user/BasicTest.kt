package co.touchlab.sqliter.user

import co.touchlab.sqliter.DatabaseConfiguration
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BasicTest{
    lateinit var database: Database

    @BeforeTest
    fun setup(){
        val man = createDatabaseManager(DatabaseConfiguration(
            name = "testdb",
            version = 1,
            inMemory = true,
            create = {
                val instance = wrapDatabaseInstance(it)
                instance.execute(TWO_COL)
            }
        ))

        database = Database(man)
    }

    @AfterTest
    fun tearDown(){
        database.close()
    }



    @Test
    fun sanityCheck(){
        val instance = database.instance()
        val stmt = instance.statement("insert into test(num, str)values(?,?)")

        stmt.execute {
            long(123)
            string("Heyo")
        }

        instance.query("select * from test where num = ?"){
            long(123)
        }.iterator().forEach {
            assertEquals("Heyo", it.getString(1))
        }
    }
}