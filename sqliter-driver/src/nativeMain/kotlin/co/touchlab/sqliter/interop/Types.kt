package co.touchlab.sqliter.interop

import cnames.structs.sqlite3_stmt
import kotlinx.cinterop.CPointer

typealias SqliteDatabasePointer = CPointer<cnames.structs.sqlite3>
typealias SqliteStatementPointer = CPointer<sqlite3_stmt>