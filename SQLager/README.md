# SQLager User Library

A simple and reasonably safe sqlite library. The design goal is to make a simple library that manages
access to sqlite and keep the lifecycle in check. Where the base SQLiter library is intended for
other libraries to use and gives pretty direct access to the sqlite C API, this library intends to
be the opposite.

The basic design is that the library doesn't let you hold onto sqlite resources. Most interaction
is through lambda functions.

## Usage

Access to sqlite is provided by a [Database](src/commonMain/kotlin/co/touchlab/sqliter/user/Database.kt)
instance. You initialize a Database by passing a SQLiter DatabaseManager. You can use other
libraries and this library together with the same DatabaseManager.

As with other sqlite libraries, you can close the Database instance, or hold onto it for the
duration of the process.

Database objects give you [DatabaseInstance](src/commonMain/kotlin/co/touchlab/sqliter/user/DatabaseInstance.kt).
This is where you call database interaction methods.

There are 4 basic interactions with sqlite. Insert, Update/Delete, Execute, and Select. If
parameters need binding, you do that by way of a lambda. The 'this' for the lambda is a
[Binder](src/commonMain/kotlin/co/touchlab/sqliter/user/Binder.kt) instance.

```kotlin
instance.insert("insert into test(num, str)values(?,?)"){
    long(123)
    string("Heyo")
}
```

The binding types are null, long, string, double, and bytes (AKA BLOB). You can bind by number or
string index. You can also omit the index and the library will count and automatically index
parameters (mixing auto and explicit numeric will probably not end well, however).

Selects do not return a result set. This is to avoid leaking sqlite statements.
Instead you provide a lambda that takes an iterator.

```kotlin
instance.query("select * from test where num = ?",{
    long(123)
}){
    it.forEach {
        assertEquals("Heyo", it.string(1))
    }
}
```

The iterator has elements of type [Row](src/commonMain/kotlin/co/touchlab/sqliter/user/Row.kt).

Transactions run inside a lambda as well. Call 'transaction' on the DatabaseInstance.

To optimize batch calls, you can hold onto a sqlite statement by using a lambda as well.

```kotlin
instance.transaction {
    it.useStatement("insert into test(num, str)values(?,?)"){
        for(i in 0 until 20000){
            long(123)
            string("Heyo")
            insert()
        }
    }
}
```
