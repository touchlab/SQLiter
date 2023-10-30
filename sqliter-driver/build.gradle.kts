import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    kotlin("multiplatform")
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
            else -> listOf("-linker-options", "-lsqlite3")
        }
    }
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
    )

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
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
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
        val linuxMain = sourceSets.maybeCreate("linuxX64Main").apply {
            dependsOn(nativeCommonMain)
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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xexpect-actual-classes"
}

if(!HostManager.hostIsLinux) {
    tasks.findByName("linuxX64Test")?.enabled = false
    tasks.findByName("linkDebugTestLinuxX64")?.enabled = false
    tasks.findByName("publishLinuxX64PublicationToMavenRepository")?.enabled = false
}

if(!HostManager.hostIsMingw) {
    tasks.findByName("mingwX64Test")?.enabled = false
    tasks.findByName("linkDebugTestMingwX64")?.enabled = false
    tasks.findByName("publishMingwX64PublicationToMavenRepository")?.enabled = false
}

apply(from = "../gradle/gradle-mvn-mpp-push.gradle")

tasks.register("publishMac"){
    setDependsOn(tasks.filter { t -> t.name.startsWith("publish") && t.name.endsWith("ToMavenRepository") && !t.name.contains("Linux") }.map { it.name })
}
