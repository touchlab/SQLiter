package co.touchlab.sqliter.user

interface Operations{
    fun execute(sql: String, bind: Binder.() -> Unit = {})
    fun insert(sql: String, bind: Binder.() -> Unit = {}): Long
    fun updateDelete(sql: String, bind: Binder.() -> Unit = {}): Int
    fun useStatement(sql: String, block: BinderStatement.() -> Unit)
    fun <T> query(sql: String, bind: Binder.() -> Unit = {}, results: (Iterator<Row>) -> T)
    fun longForQuery(sql: String): Long
    fun stringForQuery(sql: String): String
}