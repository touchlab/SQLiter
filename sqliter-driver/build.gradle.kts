import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.ByteArrayOutputStream

plugins {
    kotlin("multiplatform")
    id("com.vanniktech.maven.publish") version "0.25.3"
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

fun configInterop(target: org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget) {
    val main by target.compilations.getting
    val sqlite3 by main.cinterops.creating {
        includeDirs("$projectDir/src/include")
//      extraOpts = listOf("-mode", "sourcecode")
    }

    target.compilations.forEach { kotlinNativeCompilation ->
        kotlinNativeCompilation.kotlinOptions.freeCompilerArgs += when {
            HostManager.hostIsLinux -> listOf(
                "-linker-options",
                "-lsqlite3 -L/usr/lib/x86_64-linux-gnu -L/usr/lib"
            )

            HostManager.hostIsMingw -> listOf("-linker-options", "-lsqlite3 -Lc:\\msys64\\mingw64\\lib")
            else -> emptyList()
        }
    }
}

kotlin {
    jvmToolchain(11)
}

kotlin {
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

    if (HostManager.hostIsMac) {
        val externalLinkageTask = tasks.register("testExternalLinkage")
        tasks.named("allTests").configure {
            dependsOn(externalLinkageTask)
        }
        testExternalLinkage(macosArm64(), "arm64-apple-macosx", "macosx", externalLinkageTask)
        testExternalLinkage(macosX64(), "x86_64-apple-macosx", "macosx", externalLinkageTask)
        testExternalLinkage(iosArm64(), "arm64-apple-ios", "iphoneos", externalLinkageTask)
        testExternalLinkage(iosSimulatorArm64(), "arm64-apple-ios-simulator", "iphonesimulator", externalLinkageTask)
        testExternalLinkage(iosX64(), "x86_64-apple-ios-simulator", "iphonesimulator", externalLinkageTask)
    }

    knTargets
        .forEach { target ->
            configInterop(target)
        }

    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlin.experimental.ExperimentalNativeApi")
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
                optIn("kotlinx.cinterop.BetaInteropApi")
            }
        }
        commonMain {
            dependencies {
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val nativeCommonMain = sourceSets.maybeCreate("nativeCommonMain")
        val nativeCommonTest = sourceSets.maybeCreate("nativeCommonTest")

        val appleMain = sourceSets.maybeCreate("appleMain").apply {
            dependsOn(nativeCommonMain)
        }
        val linuxMain = sourceSets.maybeCreate("linuxMain").apply {
            dependsOn(nativeCommonMain)
        }
        val linuxX64Main = sourceSets.maybeCreate("linuxX64Main").apply {
            dependsOn(linuxMain)
        }
        val linuxArm64Main = sourceSets.maybeCreate("linuxArm64Main").apply {
            dependsOn(linuxMain)
        }

        val mingwMain = sourceSets.maybeCreate("mingwMain").apply {
            dependsOn(nativeCommonMain)
        }

        val mingwX64Main = sourceSets.maybeCreate("mingwX64Main").apply {
            dependsOn(mingwMain)
        }

        knTargets.forEach { target ->
            when {
                target.name.startsWith("mingw") -> {
                    target.compilations.getByName("main").defaultSourceSet.dependsOn(mingwMain)
                    target.compilations.getByName("test").defaultSourceSet.dependsOn(nativeCommonTest)
                }

                target.name.startsWith("linux") -> {
                    target.compilations.getByName("test").defaultSourceSet.dependsOn(nativeCommonTest)
                }

                else -> {
                    target.compilations.getByName("main").defaultSourceSet.dependsOn(appleMain)
                    target.compilations.getByName("test").defaultSourceSet.dependsOn(nativeCommonTest)
                }
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile> {
    kotlinOptions.freeCompilerArgs += "-Xexpect-actual-classes"
}

listOf(
    "linuxX64Test",
    "linuxArm64Test",
    "linkDebugTestLinuxX64",
    "linkDebugTestLinuxArm64",
    "mingwX64Test",
    "linkDebugTestMingwX64",
).forEach { tasks.findByName(it)?.enabled = false }

fun testExternalLinkage(
    target: KotlinNativeTarget,
    clangTriple: String,
    sdkName: String,
    aggregateTask: TaskProvider<*>,
) {
    target.binaries.framework(buildTypes = listOf(NativeBuildType.DEBUG)) {
        val framework = this
        baseName = "sqliteDriver"
        isStatic = true

        val testTask = tasks.register("testExternalLinkage${target.name}") {
            val sourceFilesDirectory = layout.projectDirectory.dir("externalLinkageSources")
            inputs.files(framework.linkTask, sourceFilesDirectory)
            val output = layout.buildDirectory.file("testExternalLinkage/${target.name}")
            outputs.dir(output)
            doLast {
                val sysrootPath: String = ByteArrayOutputStream().use { outputStream ->
                    project.exec {
                        commandLine("xcrun", "-sdk", sdkName, "--show-sdk-path")
                        standardOutput = outputStream
                    }
                    outputStream.toString().dropLast(1)
                }

                exec {
                    commandLine(
                        "clang", "-target", clangTriple, "-fmodules", "-ObjC",
                        "-isysroot", sysrootPath,
                        "-F", framework.linkTask.outputFile.get().parentFile,
                        sourceFilesDirectory.file("lib.m"),
                        "-dynamiclib", "-o", output.get().asFile.resolve("output.dylib")
                    )
                }
            }
        }

        aggregateTask.configure {
            dependsOn(testTask)
        }
    }
}