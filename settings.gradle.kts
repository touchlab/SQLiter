rootProject.name = "sqliter"

include(":sqliter-driver")

pluginManagement {
  val KOTLIN_VERSION: String by settings
  plugins {
    kotlin("multiplatform") version KOTLIN_VERSION
  }
}