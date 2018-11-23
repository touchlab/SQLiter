# SQLiter

SQLiter is a SQLite driver for Kotlin Multiplatform, with the intended targets of JVM/Android and all flavors
of Native that support the sqlite3 c libraries.

## Design

The basic design principles are as follows:

1. The sqlite3 libraries are pretty good as published
2. Most users access SQLite through another layer, AKA a "machined" API, so we should have access to an 
unopinionated, if not super user friendly, interface.

The intended consumer of this library is another library that is going to be used by the developer. Not that you can't
manually call the library. Just that in cases where it might be simpler or safer to hide something from 
the user vs giving full access to the underlying sqlite3 api, we lean toward the latter.

The sqlite3 c API is pretty large and has a lot of features and settings. Our goal is not to provide 
comprehensive access, nor be usable in all scenarios. Our goal is to provide a minimal api suitable
to local client applications, particularly mobile. Initially this will be specifically designed 
for SQLDelight and any other SQLite libraries intended to target Kotlin Multiplatform.

## Why?

The vast majority of database access on Android goes through the baked-in SQLiteDatabase stack. This stack was designed
a decade ago, and is trying to do a bunch of stuff to hide the details of sqlite3 and/or compensate for JNI.
There are structures that seem a little outdated (pre-WAL), and perhaps were more important on earlier 
versions of Android and hardware.

Our first native and multiplatform sqlite driver approach was [KNArch.db](https://github.com/touchlab/knarch.db). The idea
was to take AOSP SQLiteDatabase sources and port that to Kotlin/Native. That certainly works, but it's also carrying over
the older concepts and designs. Our current approach is to build from the ground up.

## Status and Plan

Currently SQLiter is in a very early state. Only iOS and mac Native implementations are active. As the native interface
solidifies we'll add the Android interface.

## Usage

SQLiter releases are published to Maven Central. You'll need a minimum gradle version of 4.10 due
to gradle metadata versioning.

```
implementation 'co.touchlab:sqliter:0.4.1'

```

## Primary Interfaces

### DatabaseManager

Creates DatabaseConnection instances. If creating your first connection, it'll manage the create/update
process. You create a DatabaseManager by calling createDatabaseManager with a DatabaseConfiguration instance.

```kotlin
val manager = createDatabaseManager(
    DatabaseConfiguration(
        name = TEST_DB_NAME, 
        version = 1,
        journalMode = JournalMode.WAL, 
        create = { db ->
            db.withStatement(TWO_COL) {
                execute()
            }
        }
    )
)
```

### DatabaseConnection

Represents a file handle connection to sqlite. You are responsible for holding onto these and if you
want to fully close references to sqlite, manually close this connection.

Connections provide the transaction management methods, and create statements. All interaction with 
sqlite happens with statements.

### Statement

Statement provides all interaction with the db. This includes query and insert/update/delete. You 
can bind parameters by index (1-based) or named.

### Cursor

A query returns a Cursor. Cursors are forward-only, as they are a thin wrapper on top of sqlite. One 
of the major reasons SQLiter exists is to avoid the CursorWindow structure of Android's sqlite driver, which
allocates a significant chunk of memory and can be problematic in some cases. SQLiter's cursor model
is simpler.

Cursors should not be shared between threads. Also, each Cursor is backed by a Statement. You should 
be careful not to try to run multiple queries on the same Statement without understanding the underlying
structure.

