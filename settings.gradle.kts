rootProject.name = "sqliter"

include(":sqliter-driver")

pluginManagement {
  repositories {
    google()
    gradlePluginPortal()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
  }
  val KOTLIN_VERSION: String by settings
  plugins {
    kotlin("multiplatform") version KOTLIN_VERSION
  }
}

enableFeaturePreview("GRADLE_METADATA")
