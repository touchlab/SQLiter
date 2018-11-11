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

The vast majority of database access on Android goes through the baked in SQLiteDatabase stack. This stack was designed
a decade ago, and is trying to do a bunch of stuff to hide the details of sqlite3 and/or compensate for JNI.
There are structures that seem a little outdated (pre-WAL), and perhaps were more important on earlier 
versions of Android and hardware. On the Android side, it seems like there might be some improvements to be
gained if we start with a lower level and add back in some features if needed.

On the native side, without the overhead of JNI, it's very difficult to imagine any case where performance 
would be improved with surrogate structures.

### Relationship to KNArch.db?

Our first attempt at a Kotlin/Native sqlite library was KNArch.db. The implementation plan was to take 
the Android AOSP Sqlite implementation, port it to Kotlin/Native, and decide what features to keep and
what to remove.

The general idea is that there's a lot of accumulated knowledge in the AOSP implementation. While that
is certainly true, there's also a decade of backward compatibility concerns and cruft baked into 
the AOSP stack.

KNArch.db is certainly still usable. Our plan going forward is to start with a clean implementation and 
add AOSP features and interfaces as desired if they have a purpose.

## Status and Plan

Currently SQLiter is in a very early state. Only iOS and mac Native implementations are currently active.
The plan is to review and add features to the native implementation, then decide if a JVM/Android 
implementation would be meaningful. While certainly possible, without measurable performance and flexibility
gains, an alternate sqlite driver for Android may be a tough sell.

