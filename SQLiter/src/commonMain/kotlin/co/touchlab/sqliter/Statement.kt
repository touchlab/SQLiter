package co.touchlab.sqliter

interface Statement{
    fun execute()
    fun executeInsert():Long
    fun executeUpdateDelete():Int
    fun query():Cursor
    fun finalize()
    fun reset()
    fun bindNull(index:Int)
    fun bindLong(index:Int, value:Long)
    fun bindDouble(index:Int, value:Double)
    fun bindString(index:Int, value:String)
    fun bindBlob(index:Int, value:ByteArray)
}
