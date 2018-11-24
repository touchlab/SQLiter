package co.touchlab.sqlager.user

import co.touchlab.sqliter.Cursor
import co.touchlab.sqliter.FieldType

internal class Results constructor(private val cursor: Cursor): Row {
    fun next(): Boolean = cursor.next()
    override fun isNull(index: Int): Boolean = cursor.isNull(index)
    override fun string(index: Int): String = cursor.getString(index)
    override fun long(index: Int): Long = cursor.getLong(index)
    override fun bytes(index: Int): ByteArray = cursor.getBytes(index)
    override fun double(index: Int): Double = cursor.getDouble(index)
    override fun type(index: Int): FieldType = cursor.getType(index)
    override val columnCount: Int = cursor.columnCount
    override fun columnName(index: Int): String = cursor.columnName(index)
    override val columnNames: Map<String, Int> = cursor.columnNames

    fun iterator():Iterator<Row> = ResultsIterator()

    internal inner class ResultsIterator():Iterator<Row>{
        var hasNextResult : Boolean? = null

        override fun hasNext(): Boolean {
            if(hasNextResult == null)
            {
                hasNextResult = this@Results.next()
            }
            return hasNextResult!!
        }

        override fun next(): Row {
            if(hasNextResult == null)
                hasNext()
            hasNextResult = null

            return this@Results
        }
    }
}
