package co.touchlab.sqlager.user

import co.touchlab.sqliter.DatabaseConfiguration
import kotlin.test.*

class BasicTest{
    lateinit var database: Database

    @BeforeTest
    fun setup(){
        val man = createDatabaseManager(DatabaseConfiguration(
            name = makeDbName(),
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
        assertTrue(database.close())
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