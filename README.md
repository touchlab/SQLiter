# SQLiter

Minimal sqlite for Kotlin multiplatform

![example workflow name](https://github.com/touchlab/SQLiter/workflows/build/badge.svg)

> ## Touchlab's Hiring!
>
> We're looking for a Mobile Developer, with Android/Kotlin experience, who is eager to dive into Kotlin Multiplatform Mobile (KMM) development. Come join the remote-first team putting KMM in production. [More info here](https://go.touchlab.co/careers-gh).

SQLiter is a SQLite driver for Kotlin Native, currently Apple and Windows variants. SQLiter powers the SQLDelight library
on native clients.


SQLiter is designed to serve as a driver to power user-friendly libraries rather than something to use directly but if you ever need it, you can find it on mavenCentral:

```
dependencies {
  implementation("co.touchlab:sqliter-driver:$version")
}
```
