package co.touchlab.sqliter

fun Cursor.iterator():CursorIterator = CursorIterator(this)

class Row{
    val values = mutableListOf<Pair<FieldType, Any?>>()
}

class CursorIterator(private val cursor: Cursor):Iterator<Row> {
    var hadNext = cursor.next()

    override fun hasNext(): Boolean = hadNext

    override fun next(): Row {
        val result = Row()
        for(i in 0 until cursor.columnCount){
            val type = cursor.getType(i)
            val value:Any? = when(type){
                FieldType.BLOB -> cursor.getBytes(i)
                FieldType.FLOAT -> cursor.getDouble(i)
                FieldType.INTEGER -> cursor.getLong(i)
                FieldType.NULL -> null
                FieldType.TEXT -> cursor.getString(i)
            }

            result.values.add(Pair(type, value))
        }

        hadNext = cursor.next()

        return result
    }
}