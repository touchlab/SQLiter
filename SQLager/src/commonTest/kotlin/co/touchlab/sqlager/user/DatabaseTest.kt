package co.touchlab.sqlager.user

import co.touchlab.sqliter.JournalMode
import co.touchlab.stately.concurrency.AtomicInt
import co.touchlab.stately.concurrency.value

import co.touchlab.stately.freeze
import kotlin.test.*

class DatabaseTest{

    @Test
    fun execute(){
        testDatabase {
            it.execute("CREATE TABLE test2 (num INTEGER NOT NULL, " +
                    "str TEXT NOT NULL)")

            it.transaction {
                for(i in 0 until 5) {
                    it.insert("insert into test2(num, str)values(?,?)") {
                        long(i.toLong())
                        string("Hey $i")
                    }
                }
            }

            assertEquals(5, it.longForQuery("select count(*) from test2"))
        }
    }

    @Test
    fun executeSqlError(){
        testDatabase {
            assertFails {
                it.execute(
                    "CREATE TABLE test (num INTEGER NOT NULL, " +
                            "str TEXT NOT NULL)"
                )
            }
        }
    }

    @Test
    fun executeBadFormatError(){
        testDatabase {
            assertFails {
                it.execute(
                    "NOPE"
                )
            }
        }
    }

    @Test
    fun insert(){
        testDatabase(createSql = "CREATE TABLE test (" +
                "ival INTEGER NOT NULL, " +
                "dval REAL NOT NULL, " +
                "bval BLOB, " +
                "sval TEXT NOT NULL)") {database ->
            BLOBS.forEach {
                database.insert("insert into test(ival, dval, bval, sval)values(?, ?, ?, ?)"){
                    long(2)
                    double(2.0)
                    bytes(it)
                    string("two")
                }
            }
        }
    }

    @Test
    fun insertFail(){
        testDatabase {database ->
            assertFails { database.insert("insert into test(num, str)values(?, ?)"){
                int(null)
                string("Hey")
            } }
        }
    }

    @Test
    fun updateDeleteUpdate(){
        makeTen {
            it.updateDelete("update test set str = 'ASDF' where num < 4")
            assertEquals(4, it.longForQuery("select count(*) from test where str = 'ASDF'"))
        }
    }

    @Test
    fun updateDeleteDelete(){
        makeTen {
            it.updateDelete("delete from test where num < 4")
            assertEquals(6, it.longForQuery("select count(*) from test"))
        }
    }

    @Test
    fun updateDeleteFail(){
        makeTen {
            assertFails { it.updateDelete("delete from testnope where num < 4") }
        }
    }

    @Test
    fun useStatement(){
        testDatabase {
            it.useStatement("insert into test(num, str)values(?, ?)"){
                for(i in 0 until 10){
                    int(i)
                    if(i % 2 == 0) {
                        string("Hey $i")
                        insert()
                    }else{
                        string(null)
                        assertFails { insert() }
                    }
                }
            }

            assertEquals(5, it.longForQuery("select count(*) from test"))
        }
    }

    @Test
    fun query(){
        makeTen {
            it.query("select * from test where num > 5"){
                assertEquals(4, it.asSequence().sumBy { 1 })
            }
        }
    }

    @Test
    fun queryFails(){
        makeTen {
            assertFails { it.query("select * from asdf"){} }
        }
    }

    @Test
    fun querySameStatement(){
        makeTen {
            var binder:Binder? = null

            it.query("select * from test where num > ?",{
                binder = this
            }){}

            it.query("select * from test where num > ?", {
                assertSame(binder, this, "statement wasn't cached?")
                int(6)
            }){
                assertEquals(3, it.asSequence().sumBy { 1 })
            }
        }
    }

    @Test
    fun longForQuery(){
        makeTen {
            assertEquals(5, it.longForQuery("select count(*) from test where num >= 5"))
            assertEquals(4, it.longForQuery("select avg(num) from test where num < 9"))
        }
    }

    @Test
    fun stringForQuery(){
        makeTen {
            assertEquals("Row 2,Row 3", it.stringForQuery("select group_concat(str,',') from test where num >= 2 and num <= 3"))
        }

    }

    @Test
    fun instanceThreadLocal() {
        testDatabase(instances = 3, inMemory = false) { database ->
            database.instance { outer ->
                database.instance {
                    assertSame(outer, it)
                }

                database.transaction {
                    assertSame(outer, it)
                }

                var binder :Binder? = null
                outer.insert("insert into test(num, str)values(?, ?)"){
                    long(1)
                    string("asdf")
                    binder = this
                }

                database.insert("insert into test(num, str)values(?, ?)"){
                    long(1)
                    string("asdf")
                    assertSame(binder, this)
                }
            }
        }
    }

    @Test
    fun testInstances(){
        testDatabase(instances = 3, inMemory = false){database ->
            for(i in 0 until 10){
                database.insert("insert into test(num, str)values(?, ?)"){
                    long(i.toLong())
                    string("Val $i")
                }
            }

            assertEquals(3, database.databaseInstances.size)
            assertTrue(database.close())
            database.databaseInstances.forEach { assertTrue { it.closed.value } }
        }
    }

//    @Test
    fun recycledInstancesFirst(){
        testDatabase(4){database ->
            val start = currentTimeMillis()

            val threadOps = ThreadOps<Database> {database.freeze()}
            for(i in 0 until 5) {
                threadOps.exe {

                    println("Starting ${currentTimeMillis()}")
                    it.transaction {
                        it.insert("insert into test(num, str)values(?, ?)") {
                            long(currentTimeMillis())
                            string("Hey")
                        }
                        sleep(2000)
                    }
                }
            }

            threadOps.run(5)
            var longest = 0L
            database.query("select num from test"){
                it.forEach {
                    val len = it.long(0) - start
                    println("len $len")
                    if(len > longest)
                        longest = len
                }
            }

            assertTrue(longest < 1000)
        }
    }

    @Test
    fun memoryDbSingleConnection(){
        testDatabase(
            instances = 4,
            inMemory = true,
            block = concurrencyChecker
        )
    }

    @Test
    fun walMultipleWriters(){
        testDatabase(
            instances = 4,
            inMemory = false,
            journalMode = JournalMode.WAL,
            block = concurrencyChecker
        )
    }

    @Test
    fun deleteMultipleWriters(){
        testDatabase(
            instances = 4,
            inMemory = false,
            journalMode = JournalMode.DELETE,
            block = concurrencyChecker
        )
    }

    private val concurrencyChecker = {database:Database ->
        val threadOps = ThreadOps {database.freeze()}
        val exceptions = AtomicInt(0)
        for(i in 0 until 5) {
            threadOps.exe {

                try {
                    it.transaction {
                        it.insert("insert into test(num, str)values(?, ?)") {
                            long(currentTimeMillis())
                            string("Hey")
                        }
                        sleep(700)
                    }
                } catch (e: Exception) {
                    exceptions.incrementAndGet()
                }
            }
        }

        threadOps.run(5)
        assertEquals(0, exceptions.value, "Inserts failed")
    }

    private val BLOBS = Array(6
    ) { i ->
        when (i) {
            0 -> parseBlob("86FADCF1A820666AEBD0789F47932151A2EF734269E8AC4E39630AB60519DFD8")
            1 -> ByteArray(1)
            2 -> null
            3 -> parseBlob("00")
            4 -> parseBlob("FF")
            5 -> parseBlob("D7B500FECF25F7A4D83BF823D3858690790F2526013DE6CAE9A69170E2A1E47238")
            else -> throw IllegalArgumentException("Nuh ugh")
        }
    }

    private fun parseBlob(src:String):ByteArray {
        val len = src.length
        val result = ByteArray(len / 2)
        for (i in 0 until len / 2)
        {
            val `val`:Int
            val c1 = src.get(i * 2)
            val c2 = src.get(i * 2 + 1)
            val val1 = c1.toLong().toString(16).toInt()// Character.digit(c1, 16)
            val val2 = c2.toLong().toString(16).toInt()
            `val` = (val1 shl 4) or val2
            result[i] = `val`.toByte()
        }
        return result
    }
}