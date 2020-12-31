package co.touchlab.sqlager.user

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class RowTest{
    @Test
    fun nameIndex(){
        makeTen {
            it.query("select * from test"){
                assertEquals(0, it.next().nameIndex("num"))
                assertEquals(1, it.next().nameIndex("str"))
            }
        }
    }

    @Test
    fun nameIndexNotFound(){
        makeTen {
            it.query("select * from test"){
                assertFails { it.next().nameIndex("nah") }
            }
        }
    }

    @Test
    fun badIndexFails(){
        makeTen {
            it.query("select * from test"){
                assertFails { it.next().int(2) }
            }
        }
    }


}