package co.touchlab.sqliter.interop

internal class TracingSqliteStatement(private val logger: Logger, private val delegate:SqliteStatement):SqliteStatement {
    private fun <T> logWrapper(name:String, params: List<Any?>, block:()->T):T{
        val result = block()
        logger.vWrite("statement.$name" + params.joinToString(separator = ", ", prefix = "(", postfix = ")") + "->" + result)
        return result
    }

    override fun isNull(index: Int): Boolean = logWrapper("isNull", listOf(index)) {delegate.isNull(index)}
    override fun columnGetLong(columnIndex: Int): Long = logWrapper("columnGetLong", listOf(columnIndex)) {delegate.columnGetLong(columnIndex)}
    override fun columnGetDouble(columnIndex: Int): Double = logWrapper("columnGetDouble", listOf(columnIndex)) {delegate.columnGetDouble(columnIndex)}
    override fun columnGetString(columnIndex: Int): String = logWrapper("columnGetString", listOf(columnIndex)) {delegate.columnGetString(columnIndex)}
    override fun columnGetBlob(columnIndex: Int): ByteArray  = logWrapper("columnGetBlob", listOf(columnIndex)) {delegate.columnGetBlob(columnIndex)}
    override fun columnCount(): Int  = logWrapper("columnCount", emptyList()) {delegate.columnCount()}
    override fun columnName(columnIndex: Int): String  = logWrapper("columnName", listOf(columnIndex)) {delegate.columnName(columnIndex)}
    override fun columnType(columnIndex: Int): Int  = logWrapper("columnType", listOf(columnIndex)) {delegate.columnType(columnIndex)}
    override fun step(): Boolean  = logWrapper("step", emptyList()) {delegate.step()}
    override fun finalizeStatement() = logWrapper("finalizeStatement", emptyList()) {delegate.finalizeStatement()}
    override fun bindParameterIndex(paramName: String): Int = logWrapper("bindParameterIndex", listOf(paramName)) {delegate.bindParameterIndex(paramName)}
    override fun resetStatement() = logWrapper("resetStatement", emptyList()) {delegate.resetStatement()}
    override fun clearBindings() = logWrapper("clearBindings", emptyList()) {delegate.clearBindings()}
    override fun execute() = logWrapper("execute", emptyList()) {delegate.execute()}
    override fun executeForChangedRowCount(): Int = logWrapper("executeForChangedRowCount", emptyList()) {delegate.executeForChangedRowCount()}
    override fun executeForLastInsertedRowId(): Long = logWrapper("executeForLastInsertedRowId", emptyList()) {delegate.executeForLastInsertedRowId()}
    override fun bindNull(index: Int) = logWrapper("bindNull", listOf(index)) {delegate.bindNull(index)}
    override fun bindLong(index: Int, value: Long) = logWrapper("bindLong", listOf(index, value)) {delegate.bindLong(index, value)}
    override fun bindDouble(index: Int, value: Double)= logWrapper("bindDouble", listOf(index, value)) {delegate.bindDouble(index, value)}
    override fun bindString(index: Int, value: String)  = logWrapper("bindString", listOf(index, value)) {delegate.bindString(index, value)}
    override fun bindBlob(index: Int, value: ByteArray)  = logWrapper("bindBlob", listOf(index, value)) {delegate.bindBlob(index, value)}
    override fun executeNonQuery(): Int = logWrapper("executeNonQuery", emptyList()) {delegate.executeNonQuery()}
    override fun traceLogCallback(message: String) {
        logger.vWrite(message)
        delegate.traceLogCallback(message)
    }
}