@import sqliteDriver;

void forceSqliteSymbolsLoading(NSString * path, SqliteDriverDatabaseConfiguration * configuration) {
  SqliteDriverNativeDatabaseManager * foo = [
    [SqliteDriverNativeDatabaseManager alloc]
    initWithPath: path
    configuration: configuration
  ];
  [foo createSingleThreadedConnection];
}