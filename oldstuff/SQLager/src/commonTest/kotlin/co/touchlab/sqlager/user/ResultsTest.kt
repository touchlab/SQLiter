package co.touchlab.sqlager.user

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ResultsTest{
    @Test
    fun resultIteratorHasAll(){
        makeTen {
            it.query("select * from test"){
                var count = 0
                it.forEach {
                    assertEquals(count.toLong(), it.long(0))
                    assertEquals("Row $count", it.string(1))
                    count++
                }

                assertEquals(count, 10)
            }
        }
    }

    @Test
    fun indexRangeFailure(){
        makeTen {
            it.query("select * from test"){
                val row = it.next()

                checkIndex { row.isNull(it) }
                checkIndex { row.string(it) }
                checkIndex { row.long(it) }
                checkIndex { row.bytes(it) }
                checkIndex { row.double(it) }
                checkIndex { row.type(it) }
                checkIndex { row.columnName(it) }
                checkIndex { row.int(it) }
                checkIndex { row.float(it) }
                checkIndex { row.stringOrNull(it) }
                checkIndex { row.longOrNull(it) }
                checkIndex { row.bytesOrNull(it) }
                checkIndex { row.doubleOrNull(it) }
                checkIndex { row.intOrNull(it) }
                checkIndex { row.floatOrNull(it) }
            }
        }
    }

    private fun checkIndex(block:(Int)->Unit){
        assertFails { block(-1) }
        assertFails { block(2) }
    }
}