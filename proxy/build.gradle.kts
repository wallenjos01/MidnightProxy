plugins {
    id("proxy-build")
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

application.mainClass.set("org.wallentines.mdproxy.Main")

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

tasks.named<JavaExec>("run") {

    standardInput = System.`in`
}

repositories {
    mavenCentral()
    maven("https://maven.wallentines.org/")
    maven("https://libraries.minecraft.net/")
    mavenLocal()
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

    implementation(libs.mojang.authlib)

    implementation(libs.google.guava)
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}

java {
    manifest {
        attributes(Pair("Main-Class", application.mainClass))
    }
}

tasks.withType<JavaExec> {

    workingDir = File("run")
}
