package co.touchlab.sqlager.user

import co.touchlab.sqliter.*
import co.touchlab.stately.concurrency.AtomicBoolean
import co.touchlab.stately.concurrency.AtomicInt

class BinderStatement internal constructor(internal val sql:String, internal val statement: Statement):
    Binder {
    override fun bytes(index: Int, bytes: ByteArray?) {
        statement.bindBlob(bindIndex(index), bytes)
    }

    override fun double(index: Int, double: Double?) {
        statement.bindDouble(bindIndex(index), double)
    }

    override fun long(index: Int, long: Long?) {
        statement.bindLong(bindIndex(index), long)
    }

    override fun string(index: Int, string: String?) {
        statement.bindString(bindIndex(index), string)
    }

    override fun nullArg(index: Int) {
        statement.bindNull(bindIndex(index))
    }

    override fun bindParameterIndex(name: String): Int {
        return statement.bindParameterIndex(name)
    }

    private val indexCounter = AtomicInt(0)
    private val indexedCount = AtomicBoolean(false)

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
        indexedCount.value = false
    }

    private fun bindIndex(paramIndex:Int):Int{
        return if(paramIndex == AUTO_INDEX) {
            if(indexedCount.value)
                throw IllegalArgumentException("Cannot mix indexed and auto binding")
            indexCounter.addAndGet(1)
        }else{
            indexedCount.value = true
            paramIndex
        }
    }

}
