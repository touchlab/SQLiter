rootProject.name = "sqliter"

include(":sqliter-driver")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

enableFeaturePreview("GRADLE_METADATA")
