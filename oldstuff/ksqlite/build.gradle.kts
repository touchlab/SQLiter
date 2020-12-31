plugins {
    kotlin("multiplatform") version "1.4.20"
    `maven-publish`
}

val ideaActive = System.getProperty("idea.active") == "true"

fun configInterop(target: org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget) {
    val main by target.compilations.getting
    val sqlite3 by main.cinterops.creating {
        includeDirs("$projectDir/src/include")
    }
}

if (ideaActive) {
    kotlin {
        val target = macosX64("native")
        configInterop(target)
    }
} else {
    kotlin {
        val knTargets = listOf(
            macosX64("native"),
            iosX64(),
            iosArm64(),
            iosArm32(),
            watchosArm32(),
            watchosArm64(),
            watchosX86(),
            tvosArm64(),
            tvosX64()
        )

        knTargets.forEach { configInterop(it) }

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

            val nativeMain = sourceSets.maybeCreate("nativeMain")
            val nativeTest = sourceSets.maybeCreate("nativeTest")

            knTargets.forEach { target ->
                target.compilations.getByName("main").source(nativeMain)
                target.compilations.getByName("test").source(nativeTest)
            }
        }
    }
}