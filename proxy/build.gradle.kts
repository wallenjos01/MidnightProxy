plugins {
    id("build.application")
    id("build.shadow")
}

application.mainClass.set("org.wallentines.mdproxy.Main")

repositories {
    maven("https://libraries.minecraft.net/")
}

dependencies {

    implementation(project(":api"))
    implementation(project(":api-jwt"))

    implementation(libs.midnight.cfg)
    implementation(libs.midnight.cfg.json)
    implementation(libs.midnight.cfg.nbt)

    implementation(libs.midnight.lib)
    implementation(libs.pseudonym)
    implementation(libs.pseudonym.text)
    implementation(libs.pseudonym.lang)

    implementation(libs.netty.buffer)
    implementation(libs.netty.codec)
    implementation(libs.netty.codec.haproxy)
    implementation(libs.netty.handler)
    implementation(libs.netty.transport)
    implementation(libs.netty.transport.epoll)
    implementation(libs.netty.transport.kqueue)

    implementation(libs.google.guava)
    implementation(libs.slf4j.api)
    implementation(libs.logback.core)
    implementation(libs.logback.classic)


    shadow(project(":api"))
    shadow(project(":api-jwt"))

    shadow(libs.midnight.cfg)
    shadow(libs.midnight.cfg.json)
    shadow(libs.midnight.cfg.nbt)

    shadow(libs.midnight.lib)
    shadow(libs.pseudonym)
    shadow(libs.pseudonym.text)
    shadow(libs.pseudonym.lang)

    shadow(libs.netty.buffer)
    shadow(libs.netty.codec)
    shadow(libs.netty.codec.haproxy)
    shadow(libs.netty.handler)
    shadow(libs.netty.transport)
    shadow(libs.netty.transport.epoll)
    shadow(libs.netty.transport.kqueue)

    shadow(libs.google.guava)
    shadow(libs.slf4j.api)
    shadow(libs.logback.core)
    shadow(libs.logback.classic)

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