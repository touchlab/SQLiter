package co.touchlab.sqliter

data class DatabaseConfiguration(
    val name:String,
    val version:Int,
    val create:(DatabaseConnection)->Unit,
    val upgrade:(DatabaseConnection, Int, Int)->Unit = {_,_,_->},
    val journalMode: JournalMode = JournalMode.WAL,
    val walAutocheckpoint: Long? = null,
    val busyTimeout:Int = 2500,
    val pageSize:Int? = null
)

enum class JournalMode {
    DELETE, WAL;

    companion object {
        fun forString(s: String) =
            if (s.toUpperCase().equals(WAL.name)) {
                WAL
            } else {
                DELETE
            }
    }
}