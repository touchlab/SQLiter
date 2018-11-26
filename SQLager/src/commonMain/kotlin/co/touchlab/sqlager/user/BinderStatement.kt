package co.touchlab.sqlager.user

import co.touchlab.sqliter.*
import co.touchlab.stately.concurrency.AtomicInt

class BinderStatement internal constructor(internal val sql:String, internal val statement: Statement):
    Binder {

    private val indexCounter = AtomicInt(0)

    override fun bytes(bytes: ByteArray?, index: Int, name: String?) {
        statement.bindBlob(bindIndex(index, name), bytes)
    }

    override fun double(double: Double?, index: Int, name: String?) {
        statement.bindDouble(bindIndex(index, name), double)
    }

    override fun long(long: Long?, index: Int, name: String?) {
        statement.bindLong(bindIndex(index, name), long)
    }

    override fun string(string: String?, index: Int, name: String?) {
        statement.bindString(bindIndex(index, name), string)
    }

    override fun nullArg(index: Int, name: String?) {
        statement.bindNull(bindIndex(index, name))
    }

    fun execute() {
        try {
            statement.execute()
        } finally {
            reset()
        }
    }

    fun insert(): Long {
        try {
            return statement.executeInsert()
        } finally {
            reset()
        }
    }

    fun updateDelete(): Int {
        try {
            return statement.executeUpdateDelete()
        } finally {
            reset()
        }
    }

    internal fun reset(){
        statement.resetStatement()
        statement.clearBindings()
        indexCounter.value = 0
    }

    private fun bindIndex(paramIndex:Int, paramName:String?):Int{
        if(paramName != null){
            return statement.bindParameterIndex(paramName)
        }

        return if(paramIndex == AUTO_INDEX) {
            indexCounter.addAndGet(1)
        }else{
            paramIndex
        }
    }

}
