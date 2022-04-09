plugins {
	`java-library-conventions`
	`java-multi-release-sources`
	`java-repackage-jars`
	`java-test-fixtures`
}

description = "JUnit Platform Commons"

dependencies {
	api(platform(projects.junitBom))

	compileOnlyApi(libs.apiguardian)
}

tasks.jar {
	val release9ClassesDir = sourceSets.mainRelease9.get().output.classesDirs.singleFile
	inputs.dir(release9ClassesDir).withPathSensitivity(PathSensitivity.RELATIVE)
	val release17ClassesDir = sourceSets.mainRelease17.get().output.classesDirs.singleFile
	inputs.dir(release17ClassesDir).withPathSensitivity(PathSensitivity.RELATIVE)
	doLast {
		exec {
			executable = project.the<JavaToolchainService>().launcherFor(java.toolchain).get()
				.metadata.installationPath.file("bin/jar").asFile.absolutePath
			args(
				"--update",
				"--file", archiveFile.get().asFile.absolutePath,
				"--release", "9",
				"-C", release9ClassesDir.absolutePath, ".",
				"--release", "17",
				"-C", release17ClassesDir.absolutePath, "."
			)
		}
	}
}

eclipse {
	classpath {
		sourceSets -= project.sourceSets.mainRelease9.get()
	}
}
