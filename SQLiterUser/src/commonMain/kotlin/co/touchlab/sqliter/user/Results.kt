package co.touchlab.sqliter.user

import co.touchlab.sqliter.Cursor
import co.touchlab.sqliter.FieldType

class Results internal constructor(
    private val cursor: Cursor,
    internal val binderStatement: BinderStatement,
    private val preparedStatement: BinderStatementRecycler):Row{
    fun next(): Boolean = cursor.next()
    override fun isNull(index: Int): Boolean = cursor.isNull(index)
    override fun getString(index: Int): String = cursor.getString(index)
    override fun getLong(index: Int): Long = cursor.getLong(index)
    override fun getBytes(index: Int): ByteArray = cursor.getBytes(index)
    override fun getDouble(index: Int): Double = cursor.getDouble(index)
    override fun getType(index: Int): FieldType = cursor.getType(index)
    override val columnCount: Int = cursor.columnCount
    override fun columnName(index: Int): String = cursor.columnName(index)
    override val columnNames: Map<String, Int> = cursor.columnNames
    fun close(){
        cursor.statement.resetStatement()
        preparedStatement.recycle(binderStatement)
    }

    fun iterator():Iterator<Row> = ResultsIterator()

    internal inner class ResultsIterator():Iterator<Row>{
        var hasNextResult : Boolean? = null

        override fun hasNext(): Boolean {
            if(hasNextResult == null)
            {
                val hn = this@Results.next()
                if(!hn)
                    this@Results.close()
                hasNextResult = hn
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
