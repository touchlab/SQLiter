plugins {
	kotlin("multiplatform") version "1.4.20"
}

val GROUP:String by project
val VERSION_NAME:String by project
val STATELY_VERSION:String by project

group = GROUP
version = VERSION_NAME

val ideaActive = System.getProperty("idea.active") == "true"

kotlin {
	val knTargets = if (ideaActive) {
		listOf(macosX64("nativeCommon"))
	} else {
		listOf(
			macosX64(),
			iosX64(),
			iosArm64(),
			iosArm32(),
			watchosArm32(),
			watchosArm64(),
			watchosX86(),
			tvosArm64(),
			tvosX64()
		)
	}

	knTargets.forEach { target ->
		val test by target.compilations.getting
		test.kotlinOptions.freeCompilerArgs += listOf("-linker-options", "-lsqlite3")
	}

	sourceSets {
		commonMain {
			dependencies {
				implementation("co.touchlab:stately-common:$STATELY_VERSION")
				implementation("co.touchlab:stately-concurrency:$STATELY_VERSION")
				implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
				implementation(project(":ksqlite"))
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
//		val macosMain = sourceSets.maybeCreate("macosMain")
//		val macosTest = sourceSets.maybeCreate("macosTest")

		val appleMain = sourceSets.maybeCreate("appleMain").apply {
			dependsOn(nativeCommonMain)
		}

		/*configure([mingwMain]) {
            dependsOn nativeCommonMain
        }*/

		if(!ideaActive) {
			knTargets.forEach { target ->
				target.compilations.getByName("main").source(appleMain)
				target.compilations.getByName("test").source(nativeCommonTest)
			}
		}

//		macosMain.dependsOn(appleMain)
		/*configure([*//*iosX64Main, iosArm64Main,*//* macosMain*//*, iosArm32Main, watchosArm32Main, watchosArm64Main, watchosX86Main, tvosArm64Main, tvosX64Main*//*]) {
			dependsOn appleMain
		}*/

//		macosTest.dependsOn(nativeCommonTest)
		/*configure([*//*iosX64Test, iosArm64Test, *//*macosTest*//*, iosArm32Test, watchosArm32Test, watchosArm64Test, watchosX86Test, tvosArm64Test, tvosX64Test, mingwTest*//*]) {
			dependsOn nativeCommonTest
		}*/
	}
}
	/*targets.withType<KotlinNativeTarget> {
		val targetDir = sqliteSrcFolder.resolve(konanTarget.presetName)

		val sourceFile = sqliteSrcFolder.resolve("sqlite3.c")
		val objFile = targetDir.resolve("sqlite3.o")
		val staticLibFile = targetDir.resolve("libsqlite3.a")

		val compileSQLite = tasks.register<Exec>("compileSQLite${konanTarget.presetName.capitalize()}") {
			onlyIf { HostManager().isEnabled(konanTarget) }

			dependsOn(unzipSQLiteSources)
			environment("PATH", "$llvmBinFolder;${System.getenv("PATH")}")
			if (HostManager.hostIsMac && konanTarget == KonanTarget.MACOS_X64) {
				environment("CPATH", "/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include")
			}

			inputs.file(sourceFile)
			outputs.file(objFile)

			executable(llvmBinFolder.resolve("clang").absolutePath)
			args("-c", "-Wall")

			val targetInfo = targetInfoMap.getValue(konanTarget)

			args("--target=${targetInfo.targetName}", "--sysroot=${targetInfo.sysRoot}")
			args(targetInfo.clangArgs)
			args(
					"-DSQLITE_ENABLE_FTS3",
					"-DSQLITE_ENABLE_FTS5",
					"-DSQLITE_ENABLE_RTREE",
					"-DSQLITE_ENABLE_DBSTAT_VTAB",
					"-DSQLITE_ENABLE_JSON1",
					"-DSQLITE_ENABLE_RBU"
			)
			args(
					"-I${sqliteSrcFolder.absolutePath}",
					"-o", objFile.absolutePath,
					sourceFile.absolutePath
			)
		}
		val archiveSQLite = tasks.register<Exec>("archiveSQLite${konanTarget.presetName.capitalize()}") {
			onlyIf { HostManager().isEnabled(konanTarget) }
			dependsOn(compileSQLite)

			inputs.file(objFile)
			outputs.file(staticLibFile)

			executable(llvmBinFolder.resolve("llvm-ar").absolutePath)
			args(
					"rc", staticLibFile.absolutePath,
					objFile.absolutePath
			)
			environment("PATH", "$llvmBinFolder;${System.getenv("PATH")}")
		}

		compilations {
			"main" {
				defaultSourceSet {
					kotlin.srcDir("src/nativeMain/kotlin")
				}
				cinterops.create("sqlite3") {
					includeDirs(sqliteSrcFolder)
					val cInteropTask = tasks[interopProcessingTaskName]
					cInteropTask.dependsOn(unzipSQLiteSources)
					compileSQLite.configure {
						dependsOn(cInteropTask)
					}
				}
				kotlinOptions {
					compileKotlinTask.dependsOn(archiveSQLite)
					freeCompilerArgs = listOf("-include-binary", staticLibFile.absolutePath)
				}
			}
			"test" {
				defaultSourceSet {
					kotlin.srcDir("src/nativeTest/kotlin")
				}
			}
		}*/
//	}
//}
