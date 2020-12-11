rootProject.name = "SQLiter"

include(":ksqlite", ":SQLiter")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

enableFeaturePreview("GRADLE_METADATA")
