package co.touchlab.sqliter

interface DatabaseConnection{
    fun createStatement(sql:String):Statement
    fun <R> withStatement(sql:String, proc:(Statement)->R):R
    fun beginTransaction()
    fun setTransactionSuccessful()
    fun endTransaction()
    fun <R> withTransaction(proc:(DatabaseConnection)->R):R
    fun close()
}