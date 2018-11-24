package co.touchlab.sqliter.user

import co.touchlab.sqliter.Statement

internal class BinderStatement(internal val sql:String, internal val statement: Statement):Binder{

    private var indexCounter = 0

    override fun bytes(bytes: ByteArray, index: Int, name: String?) {
        statement.bindBlob(bindIndex(index, name), bytes)
    }

    override fun double(double: Double, index: Int, name: String?) {
        statement.bindDouble(bindIndex(index, name), double)
    }

    override fun long(long: Long, index: Int, name: String?) {
        statement.bindLong(bindIndex(index, name), long)
    }

    override fun string(string: String, index: Int, name: String?) {
        statement.bindString(bindIndex(index, name), string)
    }

    override fun nullArg(index: Int, name: String?) {
        statement.bindNull(bindIndex(index, name))
    }

    internal fun reset(){
        statement.resetStatement()
        statement.clearBindings()
        indexCounter = 0
    }

    private fun bindIndex(paramIndex:Int, paramName:String?):Int{
        if(paramName != null){
            return statement.bindParameterIndex(paramName)
        }

        return if(paramIndex == AUTO_INDEX) {
            ++indexCounter
        }else{
            paramIndex
        }
    }

}

abstract class BinderStatementRecycler{
    internal abstract fun recycle(statement: BinderStatement)
}