plugins {
	kotlin("multiplatform") version "1.4.20"
	`maven-publish`
}

val sqliteVersion = "3310100"
val useSingleTarget: Boolean by rootProject.extra

val sqliteSrcFolder = buildDir.resolve("sqlite_src/sqlite-amalgamation-$sqliteVersion")


kotlin {
	macosX64("native") {
		val main by compilations.getting
		val sqlite3 by main.cinterops.creating  {
			includeDirs ("$projectDir/src/include")
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
}
