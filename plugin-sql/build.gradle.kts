import buildlogic.Utils;

plugins {
    id("build.library")
    id("build.shadow")
}

Utils.setupResources(project, rootProject, "plugin.json")

dependencies {
    compileOnly(libs.jetbrains.annotations)
    compileOnly(project(":api"))

    implementation(libs.midnight.cfg.sql)
    shadow(libs.midnight.cfg.sql)
}

val copyOutputTask = tasks.register<Copy>("copyOutputFiles") {

    dependsOn(tasks.shadowJar)
    from(tasks.shadowJar.get().archiveFile)

    val output = rootDir.resolve("build").resolve("plugins")

    into(output)
}

tasks.build {
    dependsOn(copyOutputTask)
}