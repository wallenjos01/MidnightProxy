plugins {
    id("proxy-build")
    id("proxy-shadow")
    id("application")
}

application.mainClass.set("org.wallentines.mdproxy.Main")

repositories {
    maven("https://libraries.minecraft.net/")
}

configurations.shadow {
    extendsFrom(configurations.implementation.get())
}

dependencies {

    implementation(project(":api"))
    implementation(project(":api-jwt"))

    implementation(libs.midnight.cfg)
    implementation(libs.midnight.cfg.json)
    implementation(libs.midnight.cfg.nbt)

    implementation(libs.midnight.lib)
    implementation(libs.midnight.core)

    implementation(libs.netty.all)

    implementation(libs.google.guava)
    implementation(libs.slf4j.api)
    implementation(libs.logback.core)
    implementation(libs.logback.classic)

    compileOnly(libs.jetbrains.annotations)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

java {
    manifest {
        attributes(Pair("Main-Class", application.mainClass))
    }
}

tasks.withType<JavaExec> {

    workingDir = File("run")
    standardInput = System.`in`
}

val copyOutputTask = tasks.register<Copy>("copyOutputFiles") {

    dependsOn(tasks.shadowJar)
    from(tasks.shadowJar.get().archiveFile)

    val output = rootDir.resolve("build")

    into(output)
}

tasks.build {
    dependsOn(copyOutputTask)
}