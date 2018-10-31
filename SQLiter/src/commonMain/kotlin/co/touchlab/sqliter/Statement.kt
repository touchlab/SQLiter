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

fun Statement.bindLong(index:Int, value:Long?){
    if(value == null)
        bindNull(index)
    else
        bindLong(index, value)
}

fun Statement.bindDouble(index:Int, value:Double?){
    if(value == null)
        bindNull(index)
    else
        bindDouble(index, value)
}

fun Statement.bindString(index:Int, value:String?){
    if(value == null)
        bindNull(index)
    else
        bindString(index, value)
}

fun Statement.bindBlob(index:Int, value:ByteArray?){
    if(value == null)
        bindNull(index)
    else
        bindBlob(index, value)
}
