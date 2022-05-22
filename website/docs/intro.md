---
sidebar_position: 1
---

# SQLiter

Sqlite driver for use in Kotlin/Native targets.

## Usage

SQLiter is a SQLite driver for Kotlin Native, currently Apple and Windows variants. SQLiter powers the SQLDelight library
on native clients.

SQLiter is designed to serve as a driver to power user-friendly libraries rather than something to use directly but if you ever need it, you can find it on mavenCentral:

```
dependencies {
  implementation("co.touchlab:sqliter-driver:$version")
}
```
