import build.plugin.Common

plugins {
    id("proxy-build")
}

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    maven("https://maven.wallentines.org/")
    maven("https://libraries.minecraft.net/")
    mavenLocal()
}

Common.setupResources(project, rootProject, "plugin.json")

dependencies {
    compileOnly(libs.jetbrains.annotations)
    compileOnly(project(":api"))
}

tasks.test {
    useJUnitPlatform()
}
