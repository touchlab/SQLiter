import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    kotlin("multiplatform")
    id("com.vanniktech.maven.publish") version "0.34.0"
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

kotlin {
    jvmToolchain(17)
}

kotlin {
    applyDefaultHierarchyTemplate()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    val knTargets = listOf(
        macosX64(),
        iosX64(),
        iosArm64(),
        watchosArm32(),
        watchosArm64(),
        watchosX64(),
        tvosArm64(),
        tvosX64(),
        macosArm64(),
        iosSimulatorArm64(),
        watchosSimulatorArm64(),
        tvosSimulatorArm64(),
        watchosDeviceArm64(),
        mingwX64(),
        linuxX64(),
        linuxArm64(),
    )

    knTargets.forEach { target ->
        target.compilations["main"].cinterops.create("sqlite3").apply {
            includeDirs("$projectDir/src/include")
//            extraOpts = listOf("-mode", "sourcecode")
        }

        target.compilerOptions {
            freeCompilerArgs.addAll(
                when {
                    HostManager.hostIsLinux -> listOf(
                        "-linker-options",
                        "-lsqlite3 -L/usr/lib/x86_64-linux-gnu -L/usr/lib"
                    )

                    HostManager.hostIsMingw -> listOf("-linker-options", "-lsqlite3 -Lc:\\msys64\\mingw64\\lib")
                    else -> listOf("-linker-options", "-lsqlite3")
                }
            )
        }
    }

    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.experimental.ExperimentalNativeApi")
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
                optIn("kotlinx.cinterop.BetaInteropApi")
            }
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

mavenPublishing {
    // Signing and POM are automatically handled by the plugin + gradle.properties
    configureBasedOnAppliedPlugins(true, true)
    publishToMavenCentral(automaticRelease = true)
}

val disabledTestLinks = mutableListOf("linkDebugTestLinuxArm64")

if (!HostManager.hostIsLinux) {
    disabledTestLinks += "linkDebugTestLinuxX64"
}

if (!HostManager.hostIsMingw) {
    disabledTestLinks += "linkDebugTestMingwX64"
}

disabledTestLinks.forEach { tasks.findByName(it)?.enabled = false }
