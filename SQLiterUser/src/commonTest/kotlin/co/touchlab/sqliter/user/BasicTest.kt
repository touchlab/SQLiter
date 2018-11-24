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

        database.insert("insert into test(num, str)values(?,?)"){
            long(123)
            string("Heyo")
        }

        /*instance.query("select * from test where num = ?",{
            long(123)
        }){
            it.forEach {
                assertEquals("Heyo", it.string(1))
            }
        }*/
    }

    @Test
    fun bigInsert(){
        database.transaction {
            it.useStatement("insert into test(num, str)values(?,?)"){
                for(i in 0 until 200000){
                    long(123)
                    string("Heyo")
                    insert()
                }
            }
        }

        assertEquals(200000, database.longForQuery("select count(*) from test"))
    }
}