import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

fun configInterop(target: KotlinNativeTarget) {
    val main by target.compilations.getting
    val sqlite3 by main.cinterops.creating {
        includeDirs("$projectDir/src/include")
    }

    target.compilations.forEach { kotlinNativeCompilation ->
        kotlinNativeCompilation.kotlinOptions.freeCompilerArgs += when {
            HostManager.hostIsLinux -> listOf(
                "-linker-options",
                "-lsqlite3 -L/usr/lib/x86_64-linux-gnu -L/usr/lib"
            )
            HostManager.hostIsMingw -> listOf("-linker-options", "-lsqlite3 -Lc:\\msys64\\mingw64\\lib")
            else -> listOf("-linker-options", "-lsqlite3")
        }
    }
}

fun disableCompilation(targets: List<KotlinNativeTarget>) {
    configure(targets) {
        compilations.all {
            cinterops.all { project.tasks[interopProcessingTaskName].enabled = false }
            compileKotlinTask.enabled = false
        }
        binaries.all { linkTask.enabled = false }
        mavenPublication {
            tasks.withType(AbstractPublishToMaven::class).all {
                onlyIf { publication != this@mavenPublication }
            }
            tasks.withType(GenerateModuleMetadata::class).all {
                onlyIf { publication.get() != this@mavenPublication }
            }
        }
    }
}

kotlin {
    val knTargets = mutableListOf(
        linuxX64(),
        mingwX64("mingw"),
        macosX64(),
        iosX64(),
        iosArm64(),
        iosArm32(),
        watchosArm32(),
        watchosArm64(),
        watchosX86(),
        watchosX64(),
        tvosArm64(),
        tvosX64()
    )

    when {
        HostManager.hostIsLinux -> {
            disableCompilation(knTargets.filter { it != linuxX64() })
        }
        HostManager.hostIsMingw -> {
            disableCompilation(knTargets.filter { it != mingwX64("mingw") })
        }
        else -> {
            disableCompilation(
                listOf(
                    linuxX64(),
                    mingwX64("mingw")
                )
            )
        }
    }

    knTargets
        .forEach { target ->
            configInterop(target)
        }

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
            }
        }
        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-common")
                implementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
            }
        }

        val nativeCommonMain = sourceSets.maybeCreate("nativeCommonMain")
        val nativeCommonTest = sourceSets.maybeCreate("nativeCommonTest")

        val appleMain = sourceSets.maybeCreate("appleMain").apply {
            dependsOn(nativeCommonMain)
        }
        val linuxMain = sourceSets.maybeCreate("linuxX64Main").apply {
            dependsOn(nativeCommonMain)
        }


        val mingwMain = sourceSets.maybeCreate("mingwMain").apply {
            dependsOn(nativeCommonMain)
        }
        knTargets.forEach { target ->
            when {
                target.name.startsWith("mingw") -> {
                    target.compilations.getByName("main").source(mingwMain)
                    target.compilations.getByName("test").source(nativeCommonTest)
                }
                target.name.startsWith("linux") -> {
                    target.compilations.getByName("main").source(linuxMain)
                    target.compilations.getByName("test").source(nativeCommonTest)
                }
                else -> {
                    target.compilations.getByName("main").source(appleMain)
                    target.compilations.getByName("test").source(nativeCommonTest)
                }
            }
        }
    }
}

apply(from = "../gradle/gradle-mvn-mpp-push.gradle")

