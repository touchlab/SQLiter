package sql

open class SQLiteException(message:String): Exception(message)
class SQLiteExceptionErr(sqliteResult: Int, sqliteError:String?): SQLiteException("$sqliteError")

//TODO: May need to create this before closing
class SQLiteExceptionHandle(db: SqliteDatabase, sqliteError:String?): SQLiteException("$sqliteError")