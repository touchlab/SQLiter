package co.touchlab.sqliter

/*
fun Cursor.iterator():Iterator<List<Any?>>{

}

internal class CursorIterator(val cursor:Cursor):Iterator<List<Any?>>{

    var hadNext = cursor.next()

    override fun hasNext(): Boolean = hadNext

    override fun next(): List<Any?> {

        val result = mutableListOf<Any?>()
        for(i in 0 until cursor.columnCount){
            result.add()
        }

        hadNext = cursor.next()
        if(!hadNext)
            cursor.close()

    }

}*/
