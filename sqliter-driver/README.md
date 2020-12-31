# SQLiter

SQLiter is a SQLite driver for Kotlin Native, currently Apple and Windows variants. It is designed to serve as a driver 
to power user-friendly libraries rather than something to use directly. Currently SQLiter powers the SQLDelight library
on native clients.

## Design

The code very roughly evolved out of the Android SQLite driver, at least historically. SQLiter is quite simple and hides
less of the details of SQLite than the Android driver does. Consuming clients will need to understand how different config
settings affect the running of SQLite.

