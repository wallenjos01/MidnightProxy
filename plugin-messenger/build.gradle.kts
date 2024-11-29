import buildlogic.Utils;

plugins {
    id("build.library")
    id("build.shadow")
}

Utils.setupResources(project, rootProject, "plugin.json")

dependencies {
    compileOnly(libs.jetbrains.annotations)
    compileOnly(project(":api"))

    implementation(libs.smi.api)
    implementation(libs.smi.base)
    implementation(libs.smi.amqp)

    shadow(libs.smi.api)
    shadow(libs.smi.base)
    shadow(libs.smi.amqp)
    shadow(libs.rabbitmq.client) { isTransitive = false }
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