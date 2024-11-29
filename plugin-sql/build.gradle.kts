import build.plugin.Common

plugins {
    id("proxy-build")
    id("proxy-shadow")
    id("proxy-publish")
}

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17


configurations.shadow {
    extendsFrom(configurations.implementation.get())
}

Common.setupResources(project, rootProject, "plugin.json")

dependencies {
    compileOnly(libs.jetbrains.annotations)
    compileOnly(project(":api"))

    implementation(libs.midnight.cfg.sql)
}

tasks.test {
    useJUnitPlatform()
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